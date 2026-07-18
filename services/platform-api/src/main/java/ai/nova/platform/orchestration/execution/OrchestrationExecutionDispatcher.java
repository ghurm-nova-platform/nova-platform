package ai.nova.platform.orchestration.execution;

import java.util.UUID;

/**
 * Replaceable dispatch boundary for claimed orchestration tasks.
 * Implementations must use a bounded executor/queue — never unbounded.
 */
public interface OrchestrationExecutionDispatcher {

    /**
     * Submit work for a claimed task. Returns {@code false} when the bounded
     * executor rejects the task (caller must release the claim without creating attempts).
     */
    boolean dispatch(UUID taskId, Runnable work);

    /** Approximate free capacity (idle threads + remaining queue slots). */
    int approximateFreeCapacity();
}
