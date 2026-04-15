package com.eneng.suporte.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt) {
    public record Jwt(String secret, long expirationMinutes) {
    }
}
