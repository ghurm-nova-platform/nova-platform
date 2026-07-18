package ai.nova.platform.modelgateway.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Process-local bounded executor for provider invocations.
 * Thread count and queue are capped; not distributed across instances.
 */
@Configuration
public class ModelGatewayInvokeExecutorConfig {

    @Bean(name = "modelGatewayInvokeExecutor", destroyMethod = "shutdown")
    public ExecutorService modelGatewayInvokeExecutor(ModelGatewayProperties properties) {
        int poolSize = Math.max(1, properties.getInvokeExecutorPoolSize());
        int queueCapacity = Math.max(1, properties.getInvokeExecutorQueueCapacity());
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("model-gateway-invoke-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                factory,
                new ThreadPoolExecutor.AbortPolicy());
    }
}
