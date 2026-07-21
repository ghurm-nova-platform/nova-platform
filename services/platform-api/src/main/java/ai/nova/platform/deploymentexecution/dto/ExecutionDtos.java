package ai.nova.platform.deploymentexecution.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.entity.ExecutionStepStatus;

public final class ExecutionDtos {

    private ExecutionDtos() {
    }

    public record CreateExecutionRequest(
            @NotNull UUID releaseId,
            @NotNull UUID environmentId,
            UUID deploymentObservationId,
            ExecutionProviderCode provider,
            @Size(max = 500) String restDeployUrl) {
    }

    public record TimelineEvent(String eventType, Instant at, String detail) {
    }

    public record ValidationCheck(UUID id, String checkCode, boolean passed, String message, Instant createdAt) {
    }

    public record StepView(
            UUID id,
            String stepKey,
            String stage,
            ExecutionStepStatus status,
            int sortOrder,
            String detail,
            Instant startedAt,
            Instant finishedAt) {
    }

    public record LogEntry(UUID id, ExecutionLogLevel level, String message, Instant createdAt) {
    }

    public record ArtifactView(
            UUID id, String artifactType, String name, String contentRef, String checksum, Instant createdAt) {
    }

    public record ResultView(
            UUID id, boolean success, String summary, String providerResponseJson, Instant createdAt) {
    }

    public record DeploymentExecution(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID environmentId,
            UUID deploymentObservationId,
            ExecutionProviderCode provider,
            ExecutionStatus status,
            String currentStep,
            String currentStage,
            String releaseManifestHash,
            String releaseContentFingerprint,
            String executionFingerprint,
            Instant startedAt,
            Instant finishedAt,
            Long durationMs,
            UUID triggeredBy,
            Instant createdAt,
            Instant updatedAt,
            String errorCode,
            String errorMessage,
            List<ValidationCheck> validations,
            List<StepView> steps,
            List<ArtifactView> artifacts,
            ResultView result,
            List<TimelineEvent> timeline) {
    }
}
