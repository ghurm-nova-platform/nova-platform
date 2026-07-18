package ai.nova.platform.orchestration.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.service.OrchestrationExecutionService;
import ai.nova.platform.orchestration.service.TaskClaimService;

@Component
@ConditionalOnProperty(prefix = "nova.orchestration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrchestrationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationScheduler.class);

    private final OrchestrationProperties properties;
    private final TaskClaimService claimService;
    private final OrchestrationExecutionService executionService;

    public OrchestrationScheduler(
            OrchestrationProperties properties,
            TaskClaimService claimService,
            OrchestrationExecutionService executionService) {
        this.properties = properties;
        this.claimService = claimService;
        this.executionService = executionService;
    }

    @Scheduled(fixedDelayString = "${nova.orchestration.poll-interval-ms:2000}")
    public void poll() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            claimService.reclaimExpiredClaims(properties.getClaimLimit());
            claimService.promoteDueRetries(properties.getClaimLimit());
            List<AgentOrchestrationTask> claimed = claimService.claimReadyTasks(properties.getClaimLimit());
            for (AgentOrchestrationTask task : claimed) {
                try {
                    executionService.executeClaimedTask(task.getId());
                } catch (Exception ex) {
                    log.warn("Failed to execute orchestration task {}", task.getId(), ex);
                }
            }
        } catch (Exception ex) {
            log.warn("Orchestration scheduler cycle failed", ex);
        }
    }
}
