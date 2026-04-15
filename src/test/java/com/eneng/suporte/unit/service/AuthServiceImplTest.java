package com.eneng.suporte.unit.service;

import com.eneng.suporte.config.SecurityProperties;
import com.eneng.suporte.domain.exception.BusinessRuleException;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.repository.UserRepository;
import com.eneng.suporte.security.JwtService;
import com.eneng.suporte.service.AuthService;
import com.eneng.suporte.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    UserRepository userRepository;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    JwtService jwtService = new JwtService(new SecurityProperties(
            new SecurityProperties.Jwt("dGVzdC1zZWNyZXQtZm9yLWp3dC10ZXN0cy1vbmx5LWRvLW5vdC11c2U=", 60)));

    AuthServiceImpl authService;

    @BeforeEach
    void init() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registrar_duplicado_lanca_BusinessRule() {
        when(userRepository.existsByUsername("joao")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(
                new AuthService.RegisterCommand("joao", "j@e.com", "segredo", Role.CLIENT)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void autenticar_password_errada_lanca_BadCredentials() {
        User u = User.builder()
                .id(UUID.randomUUID())
                .username("joao")
                .email("j@e.com")
                .passwordHash(passwordEncoder.encode("certo"))
                .role(Role.CLIENT)
                .build();
        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> authService.autenticar("joao", "errado"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void autenticar_valido_retorna_token() {
        User u = User.builder()
                .id(UUID.randomUUID())
                .username("joao")
                .email("j@e.com")
                .passwordHash(passwordEncoder.encode("segredo"))
                .role(Role.CLIENT)
                .build();
        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(u));

        AuthService.LoginResult result = authService.autenticar("joao", "segredo");

        assertThat(result.token()).isNotBlank();
        assertThat(result.user().getUsername()).isEqualTo("joao");
    }
}
