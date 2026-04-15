package com.eneng.suporte.api.controller;

import com.eneng.suporte.api.dto.CommentResponse;
import com.eneng.suporte.api.dto.CreateCommentRequest;
import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import com.eneng.suporte.api.mapper.CommentMapper;
import com.eneng.suporte.security.AuthenticatedUser;
import com.eneng.suporte.service.CommentService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
@Tag(name = "Comments", description = "Comentarios em tickets (publicos e internos)")
@SecurityRequirement(name = "bearer-jwt")
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentController(CommentService commentService, CommentMapper commentMapper) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    @PostMapping
    @Operation(
            summary = "Cria um comentario no ticket",
            description = "Adiciona um comentario ao ticket. CLIENT nao pode criar comentarios internos "
                    + "(internal=true). Campos de entrada sao validados com Bean Validation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comentario criado",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "CLIENT tentando criar comentario interno "
                    + "ou acessar ticket de outro usuario",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ticket nao encontrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CommentResponse> criar(
            @Parameter(description = "Id do ticket", required = true)
            @PathVariable UUID ticketId,
            @Validated(OnCreate.class) @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                commentMapper.toResponse(
                        commentService.adicionar(ticketId, request.body(), request.internal(),
                                AuthenticatedUser.current())));
    }

    @GetMapping
    @Operation(
            summary = "Lista comentarios do ticket paginados",
            description = "Retorna comentarios paginados. CLIENT ve somente comentarios publicos "
                    + "(internal=false); AGENT e ADMIN veem todos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de comentarios"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "CLIENT acessando ticket de outro usuario",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ticket nao encontrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Page<CommentResponse> listar(
            @Parameter(description = "Id do ticket", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "Pagina, tamanho e ordenacao (Spring Data Pageable)")
            Pageable pageable) {
        return commentService.listarPorTicket(ticketId, pageable, AuthenticatedUser.current())
                .map(commentMapper::toResponse);
    }
}
