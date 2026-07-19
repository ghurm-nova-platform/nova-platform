package ai.nova.platform.repair.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.repair.entity.RepairInputSource;
import ai.nova.platform.repair.entity.RepairStatus;

public final class RepairDtos {

    private RepairDtos() {
    }

    public record RepairRunRequest(@NotNull UUID taskId) {
    }

    public record TimelineEvent(String phase, Instant at, String detail) {
    }

    public record RepairInput(
            UUID id,
            RepairInputSource sourceType,
            String sourceRef,
            int priority,
            String detail,
            Instant createdAt) {
    }

    public record RepairAction(
            UUID id, String actionType, String targetPath, String description, Instant createdAt) {
    }

    public record RepairResult(
            UUID repairId,
            RepairStatus status,
            String summary,
            Double confidence,
            String reason,
            List<String> repairedFiles,
            UUID patchResultId,
            UUID priorPatchResultId,
            int attemptNumber,
            List<RepairInput> inputs,
            List<RepairAction> actions,
            List<TimelineEvent> timeline,
            String errorCode,
            String errorMessage,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
    }

    public record RepairOperation(
            UUID id,
            UUID taskId,
            UUID projectId,
            RepairStatus status,
            int attemptNumber,
            UUID priorPatchResultId,
            UUID newPatchResultId,
            String reason,
            String summary,
            Double confidence,
            String inputFingerprint,
            List<String> repairedFiles,
            List<RepairInput> inputs,
            List<RepairAction> actions,
            String errorCode,
            String errorMessage,
            List<TimelineEvent> timeline,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
    }
}
