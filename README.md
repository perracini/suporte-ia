# Suporte IA — Sistema de Tickets com Triagem por IA

Projeto de estudo em **Java 21 + Spring Boot 3.3** que exercita, num único app,
herança/polimorfismo JPA, RBAC com JWT, Kafka (produtor/consumidor no mesmo app)
com retry + DLQ + fallback, cache, rate limiter por semáforo, Swagger,
`@RestControllerAdvice`, Bean Validation, paginação, Docker Compose e CI.

A IA é um **Ollama/Llama local** tratado como API externa (gateway + cache + rate limit).

> **Schemas completos e respostas vivas** estão no Swagger UI:
> `http://localhost:8080/swagger-ui.html`. O README explica o **porquê** de cada
> decisão; o Swagger explica o **formato exato** de cada request/response.

---

## Stack

- Java 21, Spring Boot 3.3.4, Maven
- Spring Data JPA + PostgreSQL (embarcado via Zonky na app e nos testes)
- Spring Security + JJWT 0.12
- Spring Kafka (embedded nos testes)
- Caffeine Cache
- SpringDoc OpenAPI 2.6
- Ollama (`llama3.2`) via `RestClient`
- Docker Compose (postgres, kafka KRaft, ollama, app)
- GitHub Actions

---

## Arquitetura (camadas)

```
com.eneng.suporte
├── api/              controllers REST, DTOs, mappers
├── domain/           entidades JPA, enums, exceções de domínio
├── service/          orquestração, @Transactional, interfaces + impls
├── repository/       JpaRepository + Specifications
├── gateway/          LlamaGateway (Ollama), KafkaPublisher, RateLimiter
├── messaging/        consumers Kafka, retry/DLQ, DlqReplayService
├── security/         JwtFilter, JwtService, SecurityConfig
├── config/           Cache, OpenApi, Properties, Kafka topics
└── web/              GlobalExceptionHandler, HandlerInterceptor
```

### Modelo de domínio

- `Ticket` é `abstract` com `@Inheritance(SINGLE_TABLE)` e subtipos:
  - `BugTicket` — severidade → SLA (BLOCKER 4h, MAJOR 1d, MINOR 3d, TRIVIAL 7d)
  - `FeatureTicket` — SLA fixo 15 dias
  - `QuestionTicket` — SLA 2 dias, com `category`
- Métodos polimórficos sobrescritos por cada subtipo:
  - `slaAlvo()`, `prioridadeInicialSugerida()`, `promptParaTriagem()`, `ticketType()`
- `User` (CLIENT/AGENT/ADMIN), `Comment` (com `internal`), `AiAnalysis` (1:1 com Ticket)

---

## Fluxo de triagem

1. `POST /api/v1/tickets/bugs` (CLIENT) → `TicketService.criarBug` persiste ticket.
2. Após commit, `KafkaTicketEventPublisher` publica `TicketCreatedEvent` em `ticket.created`.
3. `TicketTriageConsumer` consome → `TicketTriageOrchestrator.triar`:
   - carrega ticket numa transação read-only e monta o prompt via `promptParaTriagem()`;
   - `LlamaGateway.analisar(prompt)` passa por `LlamaRateLimiter` (semáforo, 3 permits)
     e `@Cacheable(cacheNames="llama-analysis", key=hash(prompt))`;
   - resultado vira `AiAnalysis` (upsert) e o ticket passa a `status=IN_TRIAGE` com a prioridade sugerida.
4. Se o Llama falhar, o `DefaultErrorHandler` aplica backoff exponencial (1s/2s/4s, 4 tentativas)
   e depois manda para `ticket.created.DLT`.
5. `TicketTriageDlqListener` consome a DLT, aplica fallback polimórfico
   (`prioridadeInicialSugerida()`) e enfileira o evento para replay.
6. `POST /api/v1/admin/dlq/replay` (ADMIN) drena a fila e republica os eventos no tópico principal.

---

## Por que cada conceito está aqui

Cada escolha técnica existe para resolver um problema concreto do domínio, não para
encher o currículo. Os motivos abaixo valem tanto para entender o código quanto
para justificar as decisões numa entrevista.

- **Herança JPA + polimorfismo.** Bug, feature e pergunta têm SLAs, prompts e
  heurísticas de prioridade diferentes. Modelar como subclasses de `Ticket` deixa
  o `TicketService` chamando `ticket.promptParaTriagem()` sem conhecer o tipo
  concreto — novo subtipo entra sem tocar no service (Open/Closed). `SINGLE_TABLE`
  foi escolhida por ser a estratégia com melhor performance para leitura polimórfica.
- **JWT + RBAC.** O sistema tem três papéis com permissões bem distintas
  (CLIENT só vê os próprios tickets; AGENT assume e atualiza status; ADMIN reprocessa DLQ).
  JWT HS256 stateless evita sessão no servidor e `@PreAuthorize` mantém a regra
  de autorização **ao lado** do endpoint, fácil de auditar.
- **Kafka produtor + consumidor no mesmo app.** A triagem da IA é lenta e pode
  falhar. Se fosse síncrona, o `POST /tickets` travaria ou quebraria junto. Publicar
  um evento e processar assíncronamente desacopla o tempo de resposta do cliente
  do tempo da IA. Produtor e consumidor vivem no mesmo deploy só para simplificar
  o estudo — num ambiente real seriam apps separados, mas o código já está pronto
  para essa extração.
- **Retry com backoff exponencial + DLQ.** Llama local cai, demora, alucina. Sem
  retry, uma falha transitória perde o evento; sem DLQ, uma falha permanente trava
  o consumer. O `DefaultErrorHandler` do Spring Kafka cobre os dois casos de uma
  vez. Os intervalos (1s/2s/4s, 4 tentativas) são curtos de propósito para facilitar
  testes locais.
- **Fallback polimórfico.** Mesmo com DLQ, o ticket precisa sair do limbo
  `priority=null`. O `TicketTriageDlqListener` chama `ticket.prioridadeInicialSugerida()`
  — cada subtipo calcula pela regra que já faria sentido sem IA (BugTicket usa `severity`,
  FeatureTicket usa `MEDIUM`, etc.). É polimorfismo resolvendo um problema real
  de resiliência, não enfeite didático.
- **Cache (Caffeine).** Prompts repetidos (mesmo texto de bug reportado várias vezes)
  não precisam chamar o modelo de novo. `@Cacheable` por hash do prompt zera
  latência e libera permit do rate limiter para outros tickets. TTL curto (10min)
  porque a IA não é determinística e queremos regenerar periodicamente.
- **Rate limiter por semáforo.** Ollama local atende poucos tokens por segundo.
  Permitir 100 triagens concorrentes derruba o modelo. Um `Semaphore(3, true)`
  com `tryAcquire(timeout)` é simples, justo e não precisa de biblioteca externa
  nem de Redis. Estourou timeout → `429 Too Many Requests` via
  `GlobalExceptionHandler`.
- **Swagger (SpringDoc).** Documentação **gerada do código**, nunca desatualiza.
  Todo DTO tem `@Schema`, todo endpoint tem `@ApiResponse` por código HTTP, e
  os endpoints protegidos declaram `@SecurityRequirement("bearer-jwt")` — quem
  abre o Swagger vê exatamente qual papel/token cada rota exige.
- **Bean Validation + validation groups.** Mesma entidade pode ter regras
  diferentes em criação e atualização. `OnCreate`/`OnUpdate` (em
  `api/dto/validation/ValidationGroups`) permitem exigir campos no POST sem
  exigi-los no PATCH. Valida cedo, no controller, e devolve `ProblemDetail` 400
  com a lista de campos.
- **`@RestControllerAdvice` + ProblemDetail (RFC 7807).** Um único lugar mapeia
  todas as exceções (`MethodArgumentNotValidException`, `AccessDeniedException`,
  `RateLimitExceededException`, `LlamaUnavailableException`, `EntityNotFoundException`)
  para `application/problem+json`. Controllers ficam magros; erros ficam consistentes.
- **`@Transactional`.** `criarBug`/`criarFeature`/`criarQuestion` persistem o
  ticket **antes** de publicar no Kafka — se o publish falhar, o rollback desfaz
  o insert. Publicação acontece `afterCommit` via `TransactionSynchronization`
  para não entregar evento de um ticket que nunca existiu.
- **Paginação + Specifications.** Lista de tickets cresce rápido; devolver tudo
  quebraria tela e memória. `Pageable` + `JpaSpecificationExecutor` compõem
  filtros (status, priority, type) sem explodir em `if/else` — é o mesmo padrão
  que permitiria adicionar mais filtros sem reescrever o repositório.
- **Docker Compose.** Um comando sobe postgres + kafka (KRaft, sem Zookeeper) +
  ollama + app. Reproduzir o ambiente não exige ler documentação.
- **GitHub Actions.** CI roda `mvn verify` a cada push/PR, pegando regressão
  antes do merge. Os testes de integração usam Postgres embarcado (Zonky) +
  EmbeddedKafka justamente para o pipeline não depender de Docker.
- **Camada gateway para o externo.** Nenhum `@Service` chama HTTP ou Kafka
  direto. Isso é o "D" do SOLID na prática: trocar Ollama por OpenAI
  significa criar outro adapter de `LlamaGateway`, sem tocar no service nem no
  controller.

---

## Mapeamento conceito → onde aplica

| Conceito | Local |
|---|---|
| Herança JPA (SINGLE_TABLE) | `domain/model/Ticket.java` + subtipos |
| Polimorfismo | `slaAlvo/prioridadeInicialSugerida/promptParaTriagem` |
| RBAC com JWT | `security/JwtService`, `SecurityConfig`, `JwtAuthenticationFilter`, `@PreAuthorize` |
| IA como API externa | `gateway/llama/OllamaLlamaAdapter` |
| Kafka produtor | `gateway/kafka/KafkaTicketEventPublisher` (afterCommit) |
| Kafka consumidor | `messaging/TicketTriageConsumer` |
| Retry + backoff + DLQ | `messaging/KafkaConfig` (DefaultErrorHandler + ExponentialBackOff + DLTRecoverer) |
| Fallback polimórfico | `messaging/TicketTriageDlqListener` → `TicketService.aplicarAnaliseFallback` |
| Cache | `config/CacheConfig` (Caffeine) + `@Cacheable` no adapter |
| Rate limiter (semáforo) | `gateway/llama/LlamaRateLimiter` |
| Swagger | `config/OpenApiConfig` + `@Tag/@Operation/@ApiResponse` nos controllers |
| Bean Validation + groups | DTOs em `api/dto/*` + `api/dto/validation/ValidationGroups` |
| `@RestControllerAdvice` + ProblemDetail | `web/GlobalExceptionHandler` |
| HandlerInterceptor | `web/RequestAuditInterceptor` + `WebMvcConfig` |
| `@Transactional` | `service/TicketServiceImpl`, `CommentServiceImpl`, orchestrator |
| Paginação + Specifications | `TicketController.listar` + `repository/TicketSpecifications` |
| Docker Compose | `docker-compose.yml` (postgres + kafka KRaft + ollama + app) |
| CI | `.github/workflows/ci.yml` |

---

## Como rodar

### Testes (caminho recomendado)

```bash
mvn verify
```

Executa 25 testes unitários + 11 de integração sem depender de nenhuma
infraestrutura externa. Os ITs usam:

- **Postgres embarcado real** via [Zonky](https://github.com/zonkyio/embedded-database-spring-test)
  (baixa o binário nativo do Postgres na primeira execução e cacheia em
  `~/.embedpostgresql`). Mesmo dialeto e tipos da prod — sem H2.
- **EmbeddedKafka** do `spring-kafka-test`.
- **`LlamaGateway` mockado** via `@MockBean`, sem chamar Ollama de verdade.

### Rodar o app (no Eclipse ou IDE)

1. `File → Import → Existing Maven Projects → apontar para a raiz do projeto`.
2. Suba Postgres, Kafka e Ollama locais (nativos ou como preferir).
3. `Run As → Spring Boot App` em `SuporteIaApplication`. App em
   `http://localhost:8080`, Swagger em `http://localhost:8080/swagger-ui.html`.

Os defaults do `application.yml` apontam para `localhost:5432` (Postgres),
`localhost:9092` (Kafka) e `localhost:11434` (Ollama) — se usar portas/hosts
diferentes, sobrescreva pelas variáveis de ambiente abaixo.

### docker-compose.yml

O arquivo `docker-compose.yml` existe como **exemplo de referência** (postgres +
kafka KRaft + ollama + app). Não é o caminho esperado de execução local — está
ali para documentar como o projeto seria empacotado e, eventualmente, deployado.

### Variáveis de ambiente

| Variável | Default | Uso |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost/5432/suporte/suporte/suporte` | PostgreSQL |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka |
| `OLLAMA_URL` | `http://localhost:11434` | Ollama |
| `OLLAMA_MODEL` | `llama3.2` | Modelo |
| `JWT_SECRET` | dev-only | Assinatura JWT (HS256) |

---

## Endpoints principais

Todos os endpoints (exceto `/auth/**` e `/swagger-ui.html`) exigem
`Authorization: Bearer <token>`. Respostas de erro são
`application/problem+json` (RFC 7807). Os exemplos abaixo mostram o
**caminho feliz** — o Swagger lista todos os códigos HTTP possíveis por rota.

### Auth

**`POST /api/v1/auth/register`** — registra um usuário. Público.

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "cli1",
    "email": "cli1@eneng.local",
    "password": "segredo123",
    "role": "CLIENT"
  }'
```

Resposta `201 Created`:

```json
{
  "id": "c7f3d0a5-5b8a-44b1-8f6d-1e2c77a9b3e0",
  "username": "cli1",
  "email": "cli1@eneng.local",
  "role": "CLIENT"
}
```

**`POST /api/v1/auth/login`** — autentica e devolve JWT. Público.

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "cli1", "password": "segredo123"}'
```

Resposta `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresInSeconds": 3600,
  "userId": "c7f3d0a5-5b8a-44b1-8f6d-1e2c77a9b3e0",
  "username": "cli1",
  "role": "CLIENT"
}
```

### Tickets

**`POST /api/v1/tickets/bugs`** — cria BugTicket. Qualquer papel autenticado.

```bash
curl -X POST http://localhost:8080/api/v1/tickets/bugs \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Falha no login com Google",
    "description": "Tela branca ao clicar em entrar com Google",
    "stepsToReproduce": "1. Abrir /login\n2. Clicar em Google\n3. Consent screen abre e volta em branco",
    "affectedVersion": "2.14.0",
    "severity": "MAJOR"
  }'
```

Resposta `201 Created` — o ticket nasce com `status=OPEN` e `priority=null`;
após a triagem assíncrona, `status=IN_TRIAGE` e `aiAnalysis` preenchido.

**`POST /api/v1/tickets/features`** — cria FeatureTicket.

```bash
curl -X POST http://localhost:8080/api/v1/tickets/features \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Exportar relatorio em PDF",
    "description": "Usuarios pedem exportar dashboards em PDF para auditoria",
    "businessValue": "Desbloqueia clientes enterprise que exigem relatorios formais",
    "targetVersion": "2.16.0"
  }'
```

**`POST /api/v1/tickets/questions`** — cria QuestionTicket.

```bash
curl -X POST http://localhost:8080/api/v1/tickets/questions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Como altero minha forma de pagamento?",
    "description": "Quero trocar o cartao cadastrado sem cancelar a assinatura.",
    "category": "BILLING"
  }'
```

**`GET /api/v1/tickets`** — lista paginada com filtros. CLIENT vê só os próprios.

```bash
curl "http://localhost:8080/api/v1/tickets?status=IN_TRIAGE&priority=HIGH&type=BUG&page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN"
```

Resposta `200 OK` (formato `Page<TicketResponse>` do Spring Data):

```json
{
  "content": [
    {
      "id": "a1b2c3d4-0000-1111-2222-333344445555",
      "type": "BUG",
      "title": "Falha no login com Google",
      "status": "IN_TRIAGE",
      "priority": "HIGH",
      "createdBy": "c7f3d0a5-5b8a-44b1-8f6d-1e2c77a9b3e0",
      "assignedTo": null,
      "createdAt": "2026-04-15T10:00:00Z",
      "updatedAt": "2026-04-15T10:00:05Z",
      "aiAnalysis": {
        "suggestedCategory": "auth",
        "suggestedPriority": "HIGH",
        "draftReply": "Identificamos o problema e o time ja esta investigando.",
        "confidence": 0.87,
        "modelName": "llama3.2",
        "fallback": false,
        "createdAt": "2026-04-15T10:00:05Z"
      }
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 1,
  "totalPages": 1
}
```

**`GET /api/v1/tickets/{id}`** — detalhe do ticket com `aiAnalysis`.

**`POST /api/v1/tickets/{id}/assume`** — AGENT/ADMIN. Atribui o ticket ao
agente autenticado e avança para `IN_PROGRESS`.

**`PATCH /api/v1/tickets/{id}/status`** — AGENT/ADMIN.

```bash
curl -X PATCH http://localhost:8080/api/v1/tickets/$TICKET_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status": "RESOLVED"}'
```

### Comentários

**`POST /api/v1/tickets/{id}/comments`** — cria comentário.
CLIENT não pode enviar `internal=true` (403 com ProblemDetail).

```bash
curl -X POST http://localhost:8080/api/v1/tickets/$TICKET_ID/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "body": "Estamos investigando, aguardem retorno.",
    "internal": false
  }'
```

**`GET /api/v1/tickets/{id}/comments?page=0&size=20`** — lista paginada.
CLIENT só vê comentários com `internal=false`.

### Admin

**`POST /api/v1/admin/dlq/replay`** — ADMIN. Reprocessa eventos na DLQ.

```bash
curl -X POST http://localhost:8080/api/v1/admin/dlq/replay \
  -H "Authorization: Bearer $TOKEN"
```

Resposta `200 OK`:

```json
{ "replayed": 3 }
```

### Formato de erro (RFC 7807)

```json
{
  "type": "about:blank",
  "title": "Entrada invalida",
  "status": 400,
  "detail": "O campo 'severity' e obrigatorio",
  "instance": "/api/v1/tickets/bugs"
}
```

---

## Testes

- **Unitários** (`src/test/java/.../unit`): domínio (SLAs, prompts), `LlamaRateLimiter`
  (liberação em sucesso/erro, concorrência), `TicketServiceImpl`, `JwtService`, `AuthService`.
- **Integração** (`src/test/java/.../integration`):
  - `AuthControllerIT` — register/login/validação.
  - `TicketControllerIT` — CRUD, RBAC (401 sem token), paginação, ProblemDetail.
  - `TicketTriageFlowIT` — fluxo completo Kafka → Llama → AiAnalysis e cenário de falha → DLQ → fallback com `prioridadeInicialSugerida()`.

Total: **36 testes** (25 unit + 11 integration) verdes com `mvn verify`.

---

## Estrutura de diretórios

```
suporte-ia/
├── .github/workflows/ci.yml
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── src/
    ├── main/java/com/eneng/suporte/...
    ├── main/resources/application.yml
    ├── test/java/com/eneng/suporte/...
    └── test/resources/application-test.yml
```
