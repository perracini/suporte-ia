package com.eneng.suporte.unit.service;

import com.eneng.suporte.config.SecurityProperties;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.security.JwtService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new SecurityProperties(new SecurityProperties.Jwt(
                    "dGVzdC1zZWNyZXQtZm9yLWp3dC10ZXN0cy1vbmx5LWRvLW5vdC11c2U=", 60))
    );

    @Test
    void issue_e_parse_round_trip() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("agente1")
                .role(Role.AGENT)
                .email("a@b.com")
                .passwordHash("x")
                .build();

        JwtService.IssuedToken issued = jwtService.issue(user);
        JwtService.ParsedToken parsed = jwtService.parse(issued.token());

        assertThat(issued.expiresInSeconds()).isPositive();
        assertThat(parsed.userId()).isEqualTo(user.getId());
        assertThat(parsed.username()).isEqualTo("agente1");
        assertThat(parsed.role()).isEqualTo(Role.AGENT);
    }
}
