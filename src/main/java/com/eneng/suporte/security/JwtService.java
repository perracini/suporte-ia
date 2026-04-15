package com.eneng.suporte.security;

import com.eneng.suporte.config.SecurityProperties;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(SecurityProperties properties) {
        byte[] bytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes.length >= 32 ? bytes : padTo32(bytes));
        this.expiration = Duration.ofMinutes(properties.jwt().expirationMinutes());
    }

    private byte[] padTo32(byte[] raw) {
        byte[] out = new byte[32];
        System.arraycopy(raw, 0, out, 0, Math.min(raw.length, 32));
        return out;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(expiration);
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiration.getSeconds());
    }

    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new ParsedToken(
                UUID.fromString(claims.getSubject()),
                claims.get("username", String.class),
                Role.valueOf(claims.get("role", String.class))
        );
    }

    public record IssuedToken(String token, long expiresInSeconds) {
    }

    public record ParsedToken(UUID userId, String username, Role role) {
    }
}
