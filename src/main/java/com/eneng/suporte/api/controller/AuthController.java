package com.eneng.suporte.api.controller;

import com.eneng.suporte.api.dto.LoginRequest;
import com.eneng.suporte.api.dto.LoginResponse;
import com.eneng.suporte.api.dto.RegisterRequest;
import com.eneng.suporte.api.dto.RegisterResponse;
import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticacao e registro de usuarios")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Registra um novo cliente (self-service)",
            description = "Cria um usuario com papel CLIENT. Criacao de AGENT/ADMIN nao e permitida "
                    + "por este endpoint — use o seeder de desenvolvimento ou um endpoint administrativo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado com sucesso",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido (validacao Bean Validation)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Username ou email ja cadastrado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RegisterResponse> register(
            @Validated(OnCreate.class) @RequestBody RegisterRequest request) {
        User user = authService.registrar(new AuthService.RegisterCommand(
                request.username(),
                request.email(),
                request.password(),
                Role.CLIENT
        ));
        RegisterResponse body = new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autentica um usuario e retorna um JWT",
            description = "Recebe username e senha e devolve um token Bearer JWT (HS256) "
                    + "com o papel do usuario no claim 'role'. Use o token no header Authorization: Bearer <token>."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoginResponse> login(
            @Validated @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.autenticar(request.username(), request.password());
        return ResponseEntity.ok(new LoginResponse(
                result.token(),
                result.expiresInSeconds(),
                result.user().getId(),
                result.user().getUsername(),
                result.user().getRole()
        ));
    }
}
