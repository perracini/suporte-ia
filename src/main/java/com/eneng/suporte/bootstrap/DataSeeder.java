package com.eneng.suporte.bootstrap;

import com.eneng.suporte.domain.model.QuestionCategory;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.Severity;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.repository.UserRepository;
import com.eneng.suporte.service.TicketService;
import com.eneng.suporte.service.command.CriarBugCommand;
import com.eneng.suporte.service.command.CriarFeatureCommand;
import com.eneng.suporte.service.command.CriarQuestionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketService ticketService;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      TicketService ticketService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ticketService = ticketService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DataSeeder: usuarios ja existem ({}), pulando seed.", userRepository.count());
            return;
        }

        log.info("DataSeeder: banco vazio, populando usuarios e tickets de exemplo.");

        User admin = saveUser("admin", "admin@eneng.local", "admin123", Role.ADMIN);
        User agente = saveUser("agente", "agente@eneng.local", "agente123", Role.AGENT);
        User cliente = saveUser("cliente", "cliente@eneng.local", "cliente123", Role.CLIENT);

        log.info("DataSeeder: usuarios criados — admin={}, agente={}, cliente={}",
                admin.getId(), agente.getId(), cliente.getId());

        ticketService.criarBug(new CriarBugCommand(
                "Checkout falha ao aplicar cupom",
                "Ao aplicar o cupom PROMO10 na tela de checkout, o sistema retorna 500.",
                "1) Adicionar item ao carrinho 2) Ir pro checkout 3) Digitar PROMO10 4) Clicar em aplicar",
                "2.3.1",
                Severity.MAJOR
        ), cliente);

        ticketService.criarFeature(new CriarFeatureCommand(
                "Exportar relatorio de tickets em CSV",
                "Agentes precisam exportar a lista de tickets filtrada em CSV para auditoria mensal.",
                "Reduz tempo de fechamento do relatorio mensal de 2h para 10min.",
                "2.4.0"
        ), cliente);

        ticketService.criarQuestion(new CriarQuestionCommand(
                "Como alterar o metodo de pagamento?",
                "Preciso trocar meu cartao cadastrado por um novo, nao encontrei a opcao no perfil.",
                QuestionCategory.ACCOUNT
        ), cliente);

        log.info("DataSeeder: 3 tickets de exemplo criados (bug/feature/question).");
        log.info("DataSeeder: credenciais de estudo — admin/admin123, agente/agente123, cliente/cliente123");
    }

    private User saveUser(String username, String email, String rawPassword, Role role) {
        User u = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        return userRepository.save(u);
    }
}
