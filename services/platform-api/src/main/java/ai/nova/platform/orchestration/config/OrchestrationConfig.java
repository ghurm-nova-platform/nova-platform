package ai.nova.platform.orchestration.config;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * Bounded executor for orchestration task dispatch. Rejection is handled by releasing claims.
     */
    @Bean(name = "orchestrationExecutionExecutor", destroyMethod = "shutdown")
    ExecutorService orchestrationExecutionExecutor(OrchestrationProperties properties) {
        int poolSize = Math.max(1, properties.getDispatchPoolSize());
        int queueCapacity = Math.max(1, properties.getDispatchQueueCapacity());
        AtomicInteger idx = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "orchestration-exec-" + idx.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                factory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "nova.orchestration", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class OrchestrationSchedulingConfig {
    }
}
