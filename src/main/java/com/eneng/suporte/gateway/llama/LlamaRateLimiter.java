package com.eneng.suporte.gateway.llama;

import com.eneng.suporte.config.LlamaProperties;
import com.eneng.suporte.domain.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class LlamaRateLimiter {

    private final Semaphore semaphore;
    private final long acquireTimeoutSeconds;

    public LlamaRateLimiter(LlamaProperties properties) {
        this.semaphore = new Semaphore(properties.rateLimiter().permits(), true);
        this.acquireTimeoutSeconds = properties.rateLimiter().acquireTimeoutSeconds();
    }

    public <T> T execute(Supplier<T> action) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(acquireTimeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RateLimitExceededException(
                        "Limite concorrente do Llama atingido apos " + acquireTimeoutSeconds + "s");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RateLimitExceededException("Interrompido aguardando permit do Llama");
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
