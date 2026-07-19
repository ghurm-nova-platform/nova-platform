package ai.nova.platform.rollback.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.rollback.entity.RollbackRiskLevel;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.entity.RollbackStrategy;
import ai.nova.platform.rollback.entity.RollbackValidationResult;

public final class RollbackDtos {

    private RollbackDtos() {
    }

    public record CreateRollbackRequest(
            @NotNull UUID releaseId,
            @NotNull UUID deploymentId,
            @NotNull UUID targetReleaseId,
            @NotBlank @Size(max = 40) String environment,
            @NotNull RollbackStrategy strategy,
            @Size(max = 2000) String reason,
            RollbackRiskLevel riskLevel) {
    }

    public record TimelineEvent(String eventType, Instant at, String detail) {
    }

    public record ValidationCheck(
            UUID id, String checkCode, boolean passed, String message, Instant createdAt) {
    }

    public record TargetRef(
            UUID id, UUID targetReleaseId, String targetVersion, int sortOrder, Instant createdAt) {
    }

    public record PlanView(
            UUID id,
            UUID currentReleaseId,
            UUID targetReleaseId,
            UUID deploymentId,
            String environmentCode,
            RollbackStrategy strategy,
            String reason,
            RollbackRiskLevel riskLevel,
            RollbackValidationResult validationResult,
            String validationMessage,
            boolean immutable,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record Rollback(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID deploymentId,
            UUID targetReleaseId,
            String currentVersion,
            String targetVersion,
            UUID environmentId,
            String environmentCode,
            RollbackStatus status,
            RollbackStrategy strategy,
            String rollbackPlanHash,
            UUID createdBy,
            Instant createdAt,
            Instant validatedAt,
            Instant updatedAt,
            String errorCode,
            String errorMessage,
            PlanView plan,
            List<TargetRef> targets,
            List<ValidationCheck> validations,
            List<TimelineEvent> timeline) {
    }
}
