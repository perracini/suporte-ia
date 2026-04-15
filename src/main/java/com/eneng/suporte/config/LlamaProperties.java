package com.eneng.suporte.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llama")
public record LlamaProperties(
        String baseUrl,
        String model,
        int timeoutSeconds,
        RateLimiter rateLimiter
) {
    public record RateLimiter(int permits, int acquireTimeoutSeconds) {
    }
}
