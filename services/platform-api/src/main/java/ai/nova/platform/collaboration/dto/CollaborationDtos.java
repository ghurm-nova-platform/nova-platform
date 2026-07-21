package ai.nova.platform.collaboration.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.collaboration.entity.CollaborationDecisionType;
import ai.nova.platform.collaboration.entity.CollaborationMessageType;
import ai.nova.platform.collaboration.entity.CollaborationParticipantRole;
import ai.nova.platform.collaboration.entity.CollaborationParticipantStatus;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventType;

public final class CollaborationDtos {

    private CollaborationDtos() {
    }

    public enum TaskAction {
        ASSIGN,
        COMPLETE,
        REJECT,
        REASSIGN,
        BLOCK,
        RESUME,
        CANCEL
    }

    public record SharedContext(
            UUID projectId,
            UUID repositoryId,
            String branch,
            UUID releaseId,
            UUID environmentId,
            UUID executionId,
            List<UUID> auditEventIds) {
    }

    public record SessionSummary(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID orchestrationRunId,
            String name,
            CollaborationSessionStatus status,
            boolean conflictDetected,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ParticipantView(
            UUID id,
            CollaborationParticipantRole participantRole,
            CollaborationParticipantStatus status,
            UUID currentTaskId,
            int progressPercent,
            String parallelGroup,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
    }

    public record TaskView(
            UUID id,
            String taskKey,
            String title,
            CollaborationTaskStatus status,
            UUID participantId,
            UUID dependsOnTaskId,
            UUID blockedByTaskId,
            UUID completedByParticipantId,
            String artifactRef,
            String parallelGroup,
            Instant assignedAt,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
    }

    public record MessageView(
            UUID id,
            CollaborationParticipantRole senderRole,
            CollaborationMessageType messageType,
            String content,
            UUID taskId,
            Instant createdAt) {
    }

    public record DecisionView(
            UUID id,
            CollaborationDecisionType decisionType,
            String summary,
            Map<String, Object> details,
            UUID decidedBy,
            UUID taskId,
            Instant createdAt) {
    }

    public record TimelineEventView(
            UUID id,
            CollaborationTimelineEventType eventType,
            String summary,
            CollaborationParticipantRole actorRole,
            UUID taskId,
            UUID messageId,
            UUID decisionId,
            Map<String, Object> details,
            Instant createdAt) {
    }

    public record SessionDetail(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID orchestrationRunId,
            String name,
            CollaborationSessionStatus status,
            SharedContext sharedContext,
            String parallelGroup,
            boolean conflictDetected,
            Map<String, Object> conflictDetails,
            UUID createdBy,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt,
            List<ParticipantView> participants,
            List<TaskView> tasks,
            List<MessageView> messages,
            List<DecisionView> decisions,
            List<TimelineEventView> timeline) {
    }

    public record CollaborationConfigResponse(boolean enabled, int pollingSeconds, int maxMessages) {
    }

    public record CreateSessionRequest(
            @NotNull UUID projectId,
            UUID orchestrationRunId,
            @NotBlank @Size(max = 255) String name,
            SharedContext sharedContext,
            List<CollaborationParticipantRole> participantRoles,
            List<InitialTaskRequest> initialTasks) {
    }

    public record InitialTaskRequest(
            @NotBlank @Size(max = 120) String taskKey,
            @NotBlank @Size(max = 255) String title,
            UUID dependsOnTaskId,
            String parallelGroup,
            @Size(max = 500) String artifactRef) {
    }

    public record AssignTaskRequest(
            @NotNull UUID taskId,
            TaskAction action,
            UUID participantId,
            UUID blockedByTaskId,
            UUID reassignToParticipantId,
            @Size(max = 500) String artifactRef,
            @Size(max = 80) String parallelGroup,
            @Size(max = 2000) String reason) {
    }

    public record SendMessageRequest(
            @NotNull CollaborationParticipantRole senderRole,
            @NotNull CollaborationMessageType messageType,
            @NotBlank @Size(max = 8000) String content,
            UUID taskId) {
    }

    public record RecordDecisionRequest(
            @NotNull CollaborationDecisionType decisionType,
            @NotBlank @Size(max = 500) String summary,
            Map<String, Object> details,
            UUID taskId) {
    }
}
