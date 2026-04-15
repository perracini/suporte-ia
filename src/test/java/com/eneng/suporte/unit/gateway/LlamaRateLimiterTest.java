package com.eneng.suporte.unit.gateway;

import com.eneng.suporte.config.LlamaProperties;
import com.eneng.suporte.domain.exception.RateLimitExceededException;
import com.eneng.suporte.gateway.llama.LlamaRateLimiter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlamaRateLimiterTest {

    private LlamaRateLimiter newLimiter(int permits, int timeoutSeconds) {
        return new LlamaRateLimiter(new LlamaProperties(
                "http://localhost:11434",
                "llama3.2",
                5,
                new LlamaProperties.RateLimiter(permits, timeoutSeconds)
        ));
    }

    @Test
    void execute_libera_permit_apos_retorno() {
        LlamaRateLimiter limiter = newLimiter(1, 1);

        String result = limiter.execute(() -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(limiter.availablePermits()).isEqualTo(1);
    }

    @Test
    void libera_permit_mesmo_quando_action_falha() {
        LlamaRateLimiter limiter = newLimiter(1, 1);

        assertThatThrownBy(() -> limiter.execute(() -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(RuntimeException.class);

        assertThat(limiter.availablePermits()).isEqualTo(1);
    }

    @Test
    void excede_permits_concorrentes_lanca_RateLimitExceededException() throws InterruptedException {
        LlamaRateLimiter limiter = newLimiter(1, 0);
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        Thread blocker = new Thread(() -> limiter.execute(() -> {
            holding.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            successes.incrementAndGet();
            return null;
        }));
        blocker.start();
        holding.await();

        assertThatThrownBy(() -> limiter.execute(() -> "x"))
                .isInstanceOf(RateLimitExceededException.class);

        release.countDown();
        blocker.join();
        assertThat(successes.get()).isEqualTo(1);
    }
}
