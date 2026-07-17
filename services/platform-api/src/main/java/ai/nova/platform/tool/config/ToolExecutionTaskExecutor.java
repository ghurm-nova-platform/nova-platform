package ai.nova.platform.tool.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolExecutionTaskExecutor {

    private static final int POOL_SIZE = 4;

    @Bean(name = "toolExecutionExecutor", destroyMethod = "shutdown")
    ExecutorService toolExecutionExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("tool-execution-" + thread.threadId());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(POOL_SIZE, factory);
    }
}
