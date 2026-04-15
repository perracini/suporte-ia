package com.eneng.suporte.api.controller;

import com.eneng.suporte.api.dto.DlqEntryResponse;
import com.eneng.suporte.api.dto.DlqReplayResponse;
import com.eneng.suporte.messaging.DlqReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Operacoes administrativas (apenas ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final DlqReplayService dlqReplayService;

    public AdminController(DlqReplayService dlqReplayService) {
        this.dlqReplayService = dlqReplayService;
    }

    @PostMapping("/dlq/replay")
    @Operation(
            summary = "Reprocessa mensagens da DLQ de ticket.created",
            description = "Drena a fila em memoria de eventos que foram para ticket.created.DLT "
                    + "(apos esgotar retries no consumidor) e republica cada evento no topico principal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade de eventos reenviados",
                    content = @Content(schema = @Schema(implementation = DlqReplayResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Usuario sem papel ADMIN",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DlqReplayResponse> replayDlq() {
        int count = dlqReplayService.replay();
        return ResponseEntity.ok(new DlqReplayResponse(count));
    }

    @GetMapping("/dlq")
    @Operation(
            summary = "Lista eventos pendentes na fila de replay da DLQ",
            description = "Retorna os eventos que foram desviados para ticket.created.DLT e estao "
                    + "enfileirados em memoria aguardando replay manual. Cada entrada e enriquecida "
                    + "com o titulo do ticket original quando ele ainda existe no banco."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de eventos pendentes (pode ser vazia)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DlqEntryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Usuario sem papel ADMIN",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<DlqEntryResponse>> listDlq() {
        return ResponseEntity.ok(dlqReplayService.list());
    }
}
