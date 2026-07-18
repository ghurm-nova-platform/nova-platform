package ai.nova.platform.orchestration.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
public class OrchestrationConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "nova.orchestration", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class OrchestrationSchedulingConfig {
    }
}
