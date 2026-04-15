package com.eneng.suporte.bootstrap;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.util.HashMap;
import java.util.Map;

public class EmbeddedInfraEnvironmentPostProcessor
        implements EnvironmentPostProcessor, ApplicationListener<ApplicationPreparedEvent>, Ordered {

    private static final Log log = new DeferredLog();

    private static volatile boolean started;
    private static EmbeddedPostgres postgres;
    private static EmbeddedKafkaBroker kafka;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Boolean enabled = environment.getProperty("app.embedded.enabled", Boolean.class, Boolean.TRUE);
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }
        ensureStarted();
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        overrides.put("spring.datasource.username", "postgres");
        overrides.put("spring.datasource.password", "postgres");
        overrides.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        overrides.put("spring.kafka.bootstrap-servers", kafka.getBrokersAsString());
        environment.getPropertySources().addFirst(new MapPropertySource("embedded-infra", overrides));
        application.addListeners(this);
    }

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ((DeferredLog) log).replayTo(EmbeddedInfraEnvironmentPostProcessor.class);
    }

    private static synchronized void ensureStarted() {
        if (started) {
            return;
        }
        try {
            long t0 = System.currentTimeMillis();
            postgres = EmbeddedPostgres.start();
            log.info(String.format("Embedded Postgres iniciado em %s (%dms)",
                    postgres.getJdbcUrl("postgres", "postgres"),
                    System.currentTimeMillis() - t0));

            long t1 = System.currentTimeMillis();
            kafka = new EmbeddedKafkaKraftBroker(1, 1, "ticket.created", "ticket.created.DLT")
                    .kafkaPorts(0);
            kafka.afterPropertiesSet();
            log.info(String.format("Embedded Kafka (KRaft) iniciado em %s (%dms)",
                    kafka.getBrokersAsString(),
                    System.currentTimeMillis() - t1));

            Runtime.getRuntime().addShutdownHook(new Thread(EmbeddedInfraEnvironmentPostProcessor::stopInfra,
                    "embedded-infra-shutdown"));
            started = true;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao iniciar infraestrutura embarcada (Postgres/Kafka)", e);
        }
    }

    private static void stopInfra() {
        if (kafka != null) {
            try {
                kafka.destroy();
            } catch (Exception e) {
                log.warn("Erro ao parar Embedded Kafka: " + e.getMessage());
            }
        }
        if (postgres != null) {
            try {
                postgres.close();
            } catch (Exception e) {
                log.warn("Erro ao parar Embedded Postgres: " + e.getMessage());
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
