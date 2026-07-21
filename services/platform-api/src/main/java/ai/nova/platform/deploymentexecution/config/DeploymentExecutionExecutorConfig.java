package ai.nova.platform.deploymentexecution.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bounded managed executor for deployment provider work. HTTP threads claim QUEUED→STARTING
 * then hand off; provider I/O must not run inside an open DB transaction.
 */
@Configuration
public class DeploymentExecutionExecutorConfig {

    @Bean(name = "deploymentExecutionExecutor", destroyMethod = "shutdown")
    public ExecutorService deploymentExecutionExecutor(ExecutionProperties properties) {
        int workers = Math.max(1, properties.getWorkerCount());
        int queueCapacity = Math.max(1, properties.getQueueCapacity());
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("deployment-execution-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                workers,
                workers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                factory,
                new ThreadPoolExecutor.AbortPolicy());
    }
}
