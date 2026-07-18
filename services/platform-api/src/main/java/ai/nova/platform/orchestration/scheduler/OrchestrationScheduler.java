package ai.nova.platform.orchestration.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.execution.OrchestrationExecutionDispatcher;
import ai.nova.platform.orchestration.service.OrchestrationExecutionService;
import ai.nova.platform.orchestration.service.TaskClaimService;

@Component
@ConditionalOnProperty(prefix = "nova.orchestration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrchestrationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationScheduler.class);

    private final OrchestrationProperties properties;
    private final TaskClaimService claimService;
    private final OrchestrationExecutionService executionService;
    private final OrchestrationExecutionDispatcher dispatcher;

    public OrchestrationScheduler(
            OrchestrationProperties properties,
            TaskClaimService claimService,
            OrchestrationExecutionService executionService,
            OrchestrationExecutionDispatcher dispatcher) {
        this.properties = properties;
        this.claimService = claimService;
        this.executionService = executionService;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${nova.orchestration.poll-interval-ms:2000}")
    public void poll() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            claimService.reclaimExpiredClaims(properties.getClaimLimit());
            claimService.promoteDueRetries(properties.getClaimLimit());

            int freeDispatch = dispatcher.approximateFreeCapacity();
            int claimBudget = Math.min(properties.getClaimLimit(), Math.max(0, freeDispatch));
            if (claimBudget == 0) {
                return;
            }

            List<AgentOrchestrationTask> claimed = claimService.claimReadyTasks(claimBudget);
            for (AgentOrchestrationTask task : claimed) {
                boolean accepted = dispatcher.dispatch(
                        task.getId(),
                        () -> executionService.executeClaimedTask(task.getId()));
                if (!accepted) {
                    boolean released = claimService.releaseUnstartedClaim(task.getId());
                    log.warn(
                            "Released claim for task {} after executor rejection (released={})",
                            task.getId(),
                            released);
                }
            }
        } catch (Exception ex) {
            log.warn("Orchestration scheduler cycle failed", ex);
        }
    }
}
