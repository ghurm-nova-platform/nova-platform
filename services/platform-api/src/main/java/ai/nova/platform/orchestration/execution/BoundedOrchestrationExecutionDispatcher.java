package ai.nova.platform.orchestration.execution;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class BoundedOrchestrationExecutionDispatcher implements OrchestrationExecutionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(BoundedOrchestrationExecutionDispatcher.class);

    private final ExecutorService executor;

    public BoundedOrchestrationExecutionDispatcher(
            @Qualifier("orchestrationExecutionExecutor") ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public boolean dispatch(UUID taskId, Runnable work) {
        try {
            executor.execute(() -> {
                try {
                    work.run();
                } catch (Exception ex) {
                    log.warn("Orchestration dispatch failed for task {}", taskId, ex);
                }
            });
            return true;
        } catch (RejectedExecutionException ex) {
            log.warn("Orchestration executor rejected task {}", taskId);
            return false;
        }
    }

    @Override
    public int approximateFreeCapacity() {
        if (!(executor instanceof ThreadPoolExecutor pool)) {
            return 0;
        }
        int idleWorkers = Math.max(0, pool.getMaximumPoolSize() - pool.getActiveCount());
        int freeQueue = pool.getQueue().remainingCapacity();
        return idleWorkers + freeQueue;
    }
}
