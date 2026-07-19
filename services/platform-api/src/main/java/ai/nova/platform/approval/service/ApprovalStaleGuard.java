package ai.nova.platform.approval.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.approval.entity.ApprovalDecisionEntity;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalStaleGuard {

    private final ApprovalGateProperties properties;
    private final ApprovalEvidenceCollector evidenceCollector;
    private final ApprovalFingerprint fingerprint;
    private final AgentOrchestrationTaskRepository taskRepository;

    public ApprovalStaleGuard(
            ApprovalGateProperties properties,
            ApprovalEvidenceCollector evidenceCollector,
            ApprovalFingerprint fingerprint,
            AgentOrchestrationTaskRepository taskRepository) {
        this.properties = properties;
        this.evidenceCollector = evidenceCollector;
        this.fingerprint = fingerprint;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public StaleCheckResult revalidate(UUID taskId, UUID organizationId, ApprovalDecisionEntity decision) {
        if (decision.getValidUntil() != null && Instant.now().isAfter(decision.getValidUntil())) {
            return StaleCheckResult.expired("Decision validity window elapsed");
        }

        AgentOrchestrationTask task = taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "APPROVAL_TASK_NOT_FOUND", "Task not found for approval gate"));

        ApprovalEvidenceBundle current = evidenceCollector.collect(task);
        String currentEvidenceFp = fingerprint.evidenceFingerprint(current);

        if (!currentEvidenceFp.equals(decision.getEvidenceFingerprint())) {
            if (properties.isInvalidateOnNewPatch()
                    && !sameId(decision.getPatchResultId(), current.patch() != null ? current.patch().id() : null)) {
                return StaleCheckResult.invalidated("Patch evidence changed");
            }
            if (properties.isInvalidateOnNewCommit()
                    && !sameString(decision.getCommitHash(), current.git() != null ? current.git().commitHash() : null)) {
                return StaleCheckResult.invalidated("Git commit changed");
            }
            if (properties.isInvalidateOnNewCiObservation()
                    && !sameId(
                            decision.getCiObservationOperationId(),
                            current.ci() != null ? current.ci().id() : null)) {
                return StaleCheckResult.invalidated("CI observation changed");
            }
            if (properties.isInvalidateOnPrHeadChange()
                    && current.pullRequest() != null
                    && !sameString(decision.getCommitHash(), current.pullRequest().localCommitHash())) {
                return StaleCheckResult.invalidated("Pull request head commit changed");
            }
            return StaleCheckResult.invalidated("Evidence fingerprint changed");
        }

        return StaleCheckResult.valid();
    }

    private static boolean sameId(UUID a, UUID b) {
        if (a == null && b == null) {
            return true;
        }
        return a != null && a.equals(b);
    }

    private static boolean sameString(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        return a != null && a.equals(b);
    }

    public record StaleCheckResult(
            boolean isValid, ApprovalDecisionValue overrideDecision, String reason) {

        static StaleCheckResult valid() {
            return new StaleCheckResult(true, null, null);
        }

        static StaleCheckResult expired(String reason) {
            return new StaleCheckResult(false, ApprovalDecisionValue.EXPIRED, reason);
        }

        static StaleCheckResult invalidated(String reason) {
            return new StaleCheckResult(false, ApprovalDecisionValue.INVALIDATED, reason);
        }
    }
}
