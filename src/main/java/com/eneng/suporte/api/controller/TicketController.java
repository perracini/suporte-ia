package com.eneng.suporte.api.controller;

import com.eneng.suporte.api.dto.CreateBugRequest;
import com.eneng.suporte.api.dto.CreateFeatureRequest;
import com.eneng.suporte.api.dto.CreateQuestionRequest;
import com.eneng.suporte.api.dto.TicketResponse;
import com.eneng.suporte.api.dto.UpdateStatusRequest;
import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import com.eneng.suporte.api.dto.validation.ValidationGroups.OnUpdate;
import com.eneng.suporte.api.mapper.TicketMapper;
import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.domain.model.TicketStatus;
import com.eneng.suporte.security.AuthenticatedUser;
import com.eneng.suporte.service.TicketService;
import com.eneng.suporte.service.command.CriarBugCommand;
import com.eneng.suporte.service.command.CriarFeatureCommand;
import com.eneng.suporte.service.command.CriarQuestionCommand;
import com.eneng.suporte.service.command.TicketFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets", description = "Gestao de tickets de suporte (CRUD, triagem, atribuicao)")
@SecurityRequirement(name = "bearer-jwt")
public class TicketController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    public TicketController(TicketService ticketService, TicketMapper ticketMapper) {
        this.ticketService = ticketService;
        this.ticketMapper = ticketMapper;
    }

    @PostMapping("/bugs")
    @Operation(
            summary = "Cria um ticket do tipo BUG",
            description = "Cria um BugTicket. Apos o commit, publica TicketCreatedEvent no Kafka "
                    + "para triagem assincrona pela IA (Llama). A resposta inicial pode vir com priority=null "
                    + "ate que o consumidor processe o evento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket criado",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TicketResponse> criarBug(
            @Validated(OnCreate.class) @RequestBody CreateBugRequest request) {
        Ticket ticket = ticketService.criarBug(new CriarBugCommand(
                request.title(),
                request.description(),
                request.stepsToReproduce(),
                request.affectedVersion(),
                request.severity()
        ), AuthenticatedUser.current());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketMapper.toResponse(ticket));
    }

    @PostMapping("/features")
    @Operation(
            summary = "Cria um ticket do tipo FEATURE",
            description = "Cria um FeatureTicket com SLA fixo de 15 dias e triagem assincrona via Kafka/Llama."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket criado",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TicketResponse> criarFeature(
            @Validated(OnCreate.class) @RequestBody CreateFeatureRequest request) {
        Ticket ticket = ticketService.criarFeature(new CriarFeatureCommand(
                request.title(),
                request.description(),
                request.businessValue(),
                request.targetVersion()
        ), AuthenticatedUser.current());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketMapper.toResponse(ticket));
    }

    @PostMapping("/questions")
    @Operation(
            summary = "Cria um ticket do tipo QUESTION",
            description = "Cria um QuestionTicket (SLA 2 dias). A IA gera um rascunho de resposta no draftReply."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket criado",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TicketResponse> criarQuestion(
            @Validated(OnCreate.class) @RequestBody CreateQuestionRequest request) {
        Ticket ticket = ticketService.criarQuestion(new CriarQuestionCommand(
                request.title(),
                request.description(),
                request.category()
        ), AuthenticatedUser.current());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketMapper.toResponse(ticket));
    }

    @GetMapping
    @Operation(
            summary = "Lista tickets paginados",
            description = "Retorna tickets visiveis ao usuario atual. CLIENT ve apenas os proprios; "
                    + "AGENT e ADMIN veem todos. Aceita filtros por status/priority/type e parametros "
                    + "padrao do Spring Data Pageable (page, size, sort)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de tickets"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Page<TicketResponse> listar(
            @Parameter(description = "Filtra pelo status atual do ticket", example = "IN_TRIAGE")
            @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Filtra pela prioridade", example = "HIGH")
            @RequestParam(required = false) Priority priority,
            @Parameter(description = "Filtra pelo tipo polimorfico (BUG, FEATURE, QUESTION)", example = "BUG")
            @RequestParam(required = false) String type,
            @Parameter(description = "Pagina, tamanho e ordenacao. Ex.: page=0&size=20&sort=createdAt,desc")
            Pageable pageable
    ) {
        return ticketService.listar(new TicketFilter(status, priority, type), pageable, AuthenticatedUser.current())
                .map(ticketMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um ticket por id",
            description = "Retorna o ticket com a analise da IA (se ja tiver sido triado). "
                    + "CLIENT so consegue acessar os proprios tickets."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket encontrado",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "CLIENT tentando acessar ticket de outro usuario",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ticket nao encontrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TicketResponse buscar(
            @Parameter(description = "Id do ticket", required = true)
            @PathVariable UUID id) {
        return ticketMapper.toResponse(ticketService.buscar(id, AuthenticatedUser.current()));
    }

    @PostMapping("/{id}/assume")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
            summary = "Agente assume um ticket",
            description = "Atribui o ticket ao agente autenticado e transiciona o status para IN_PROGRESS. "
                    + "Somente AGENT e ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket assumido",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Usuario sem papel AGENT/ADMIN",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ticket nao encontrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TicketResponse assumir(
            @Parameter(description = "Id do ticket", required = true)
            @PathVariable UUID id) {
        return ticketMapper.toResponse(ticketService.assumir(id, AuthenticatedUser.current()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
            summary = "Atualiza o status de um ticket",
            description = "Somente AGENT e ADMIN. Valida transicoes de status do ticket."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido ou transicao invalida",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Usuario sem papel AGENT/ADMIN",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ticket nao encontrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public TicketResponse atualizarStatus(
            @Parameter(description = "Id do ticket", required = true)
            @PathVariable UUID id,
            @Validated(OnUpdate.class) @RequestBody UpdateStatusRequest request) {
        return ticketMapper.toResponse(
                ticketService.atualizarStatus(id, request.status(), AuthenticatedUser.current()));
    }
}
