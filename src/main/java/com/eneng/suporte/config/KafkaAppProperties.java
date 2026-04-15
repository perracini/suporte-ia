package com.eneng.suporte.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaAppProperties(
        Topics topics,
        Retry retry
) {
    public record Topics(String ticketCreated, String ticketCreatedDlt) {
    }

    public record Retry(long initialIntervalMs, double multiplier, long maxIntervalMs, int maxAttempts) {
    }
}
