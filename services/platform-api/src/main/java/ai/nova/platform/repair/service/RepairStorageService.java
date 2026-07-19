package ai.nova.platform.repair.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.repair.dto.RepairDtos.RepairAction;
import ai.nova.platform.repair.dto.RepairDtos.RepairInput;
import ai.nova.platform.repair.dto.RepairDtos.RepairOperation;
import ai.nova.platform.repair.dto.RepairDtos.TimelineEvent;
import ai.nova.platform.repair.entity.RepairActionEntity;
import ai.nova.platform.repair.entity.RepairInputEntity;
import ai.nova.platform.repair.entity.RepairOperationEntity;
import ai.nova.platform.repair.entity.RepairResultEntity;
import ai.nova.platform.repair.entity.RepairStatus;
import ai.nova.platform.repair.repository.RepairActionRepository;
import ai.nova.platform.repair.repository.RepairInputRepository;
import ai.nova.platform.repair.repository.RepairOperationRepository;
import ai.nova.platform.repair.repository.RepairResultRepository;
import ai.nova.platform.repair.service.RepairInputCollector.CollectedInput;
import ai.nova.platform.web.error.ApiException;

@Service
public class RepairStorageService {

    private final RepairOperationRepository operationRepository;
    private final RepairInputRepository inputRepository;
    private final RepairActionRepository actionRepository;
    private final RepairResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    public RepairStorageService(
            RepairOperationRepository operationRepository,
            RepairInputRepository inputRepository,
            RepairActionRepository actionRepository,
            RepairResultRepository resultRepository,
            ObjectMapper objectMapper) {
        this.operationRepository = operationRepository;
        this.inputRepository = inputRepository;
        this.actionRepository = actionRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RepairOperation startPending(
            UUID operationId,
            AgentOrchestrationTask task,
            int attemptNumber,
            UUID priorPatchResultId,
            String inputFingerprint,
            String reason,
            Instant startedAt,
            List<TimelineEvent> timeline) {
        Instant now = Instant.now();
        RepairOperationEntity operation = new RepairOperationEntity(
                operationId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getId(),
                RepairStatus.PENDING,
                attemptNumber,
                priorPatchResultId,
                null,
                reason,
                null,
                null,
                inputFingerprint,
                null,
                null,
                startedAt,
                null,
                now,
                now);
        operationRepository.save(operation);
        return reloadOperation(operationId, timeline);
    }

    @Transactional
    public RepairOperation updateStatus(UUID operationId, RepairStatus status, List<TimelineEvent> timeline) {
        RepairOperationEntity operation = requireOperation(operationId);
        operation.updateStatus(status);
        operationRepository.save(operation);
        return reloadOperation(operationId, timeline);
    }

    @Transactional
    public RepairOperation saveInputs(UUID operationId, List<CollectedInput> inputs, List<TimelineEvent> timeline) {
        requireOperation(operationId);
        Instant now = Instant.now();
        for (CollectedInput input : inputs) {
            inputRepository.save(new RepairInputEntity(
                    UUID.randomUUID(),
                    operationId,
                    input.sourceType(),
                    input.sourceRef(),
                    input.priority(),
                    input.detail(),
                    now));
        }
        return reloadOperation(operationId, timeline);
    }

    @Transactional
    public RepairOperation saveActions(UUID operationId, List<RepairAction> actions, List<TimelineEvent> timeline) {
        requireOperation(operationId);
        Instant now = Instant.now();
        for (RepairAction action : actions) {
            actionRepository.save(new RepairActionEntity(
                    action.id() == null ? UUID.randomUUID() : action.id(),
                    operationId,
                    action.actionType(),
                    action.targetPath(),
                    action.description(),
                    action.createdAt() == null ? now : action.createdAt()));
        }
        return reloadOperation(operationId, timeline);
    }

    @Transactional
    public RepairOperation markSucceeded(
            UUID operationId,
            UUID newPatchResultId,
            String summary,
            double confidence,
            String reason,
            List<String> repairedFiles,
            Instant completedAt,
            List<TimelineEvent> timeline) {
        RepairOperationEntity operation = requireOperation(operationId);
        operation.markSucceeded(newPatchResultId, summary, confidence, reason, completedAt);
        operationRepository.save(operation);

        Instant now = Instant.now();
        resultRepository.save(new RepairResultEntity(
                UUID.randomUUID(),
                operationId,
                newPatchResultId,
                writeRepairedFiles(repairedFiles),
                summary,
                confidence,
                now));
        return reloadOperation(operationId, timeline);
    }

    @Transactional
    public RepairOperation markFailed(
            UUID operationId, String errorCode, String message, Instant completedAt, List<TimelineEvent> timeline) {
        RepairOperationEntity operation = requireOperation(operationId);
        operation.markFailed(errorCode, message, completedAt);
        operationRepository.save(operation);
        return reloadOperation(operationId, timeline);
    }

    @Transactional(readOnly = true)
    public RepairOperation findLatest(UUID taskId, UUID organizationId) {
        return operationRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(entity -> reloadOperation(entity.getId(), null))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<RepairOperation> findHistory(UUID taskId, UUID organizationId) {
        return operationRepository.findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId).stream()
                .map(entity -> reloadOperation(entity.getId(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countByTask(UUID taskId, UUID organizationId) {
        return operationRepository.countByTaskIdAndOrganizationId(taskId, organizationId);
    }

    @Transactional(readOnly = true)
    public RepairOperation findByFingerprint(UUID organizationId, UUID taskId, String fingerprint) {
        return operationRepository
                .findByOrganizationIdAndTaskIdAndInputFingerprint(organizationId, taskId, fingerprint)
                .map(entity -> reloadOperation(entity.getId(), null))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public RepairOperation findSucceededByFingerprint(UUID organizationId, UUID taskId, String fingerprint) {
        return operationRepository
                .findByOrganizationIdAndTaskIdAndInputFingerprintAndStatus(
                        organizationId, taskId, fingerprint, RepairStatus.SUCCEEDED)
                .map(entity -> reloadOperation(entity.getId(), null))
                .orElse(null);
    }

    private RepairOperation reloadOperation(UUID operationId, List<TimelineEvent> timelineOverride) {
        RepairOperationEntity operation = requireOperation(operationId);
        List<RepairInputEntity> inputEntities =
                inputRepository.findByRepairOperationIdOrderByPriorityAscCreatedAtAsc(operationId);
        List<RepairActionEntity> actionEntities =
                actionRepository.findByRepairOperationIdOrderByCreatedAtAsc(operationId);
        RepairResultEntity resultEntity = resultRepository.findByRepairOperationId(operationId).orElse(null);

        List<RepairInput> inputs = inputEntities.stream().map(RepairStorageService::toInput).toList();
        List<RepairAction> actions = actionEntities.stream().map(RepairStorageService::toAction).toList();
        List<String> repairedFiles = resultEntity == null ? List.of() : readRepairedFiles(resultEntity);
        List<TimelineEvent> timeline =
                timelineOverride == null ? buildTimeline(operation) : timelineOverride;
        return toOperation(operation, inputs, actions, repairedFiles, timeline);
    }

    private RepairOperationEntity requireOperation(UUID operationId) {
        return operationRepository
                .findById(operationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "REPAIR_NOT_FOUND", "Repair operation not found"));
    }

    private String writeRepairedFiles(List<String> repairedFiles) {
        try {
            return objectMapper.writeValueAsString(repairedFiles == null ? List.of() : repairedFiles);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPAIR_INVALID_STATE", "Failed to serialize repaired files");
        }
    }

    private List<String> readRepairedFiles(RepairResultEntity entity) {
        try {
            return objectMapper.readValue(entity.getRepairedFilesJson(), new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static RepairOperation toOperation(
            RepairOperationEntity operation,
            List<RepairInput> inputs,
            List<RepairAction> actions,
            List<String> repairedFiles,
            List<TimelineEvent> timeline) {
        return new RepairOperation(
                operation.getId(),
                operation.getTaskId(),
                operation.getProjectId(),
                operation.getStatus(),
                operation.getAttemptNumber(),
                operation.getPriorPatchResultId(),
                operation.getNewPatchResultId(),
                operation.getReason(),
                operation.getSummary(),
                operation.getConfidence(),
                operation.getInputFingerprint(),
                repairedFiles,
                inputs,
                actions,
                operation.getErrorCode(),
                operation.getErrorMessage(),
                timeline,
                operation.getStartedAt(),
                operation.getCompletedAt(),
                operation.getCreatedAt());
    }

    private static RepairInput toInput(RepairInputEntity entity) {
        return new RepairInput(
                entity.getId(),
                entity.getSourceType(),
                entity.getSourceRef(),
                entity.getPriority(),
                entity.getDetail(),
                entity.getCreatedAt());
    }

    private static RepairAction toAction(RepairActionEntity entity) {
        return new RepairAction(
                entity.getId(),
                entity.getActionType(),
                entity.getTargetPath(),
                entity.getDescription(),
                entity.getCreatedAt());
    }

    private static List<TimelineEvent> buildTimeline(RepairOperationEntity operation) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("STARTED", operation.getStartedAt(), "Repair agent started"));
        if (operation.getStatus().ordinal() >= RepairStatus.COLLECTING.ordinal()) {
            events.add(new TimelineEvent("COLLECTING", operation.getStartedAt(), "Collecting failure inputs"));
        }
        if (operation.getStatus().ordinal() >= RepairStatus.ANALYZING.ordinal()) {
            events.add(new TimelineEvent("ANALYZING", operation.getStartedAt(), "Analyzing failures"));
        }
        if (operation.getStatus().ordinal() >= RepairStatus.GENERATING_PATCH.ordinal()) {
            events.add(new TimelineEvent("GENERATING_PATCH", operation.getStartedAt(), "Generating repair patch"));
        }
        if (operation.getStatus().ordinal() >= RepairStatus.VALIDATING.ordinal()) {
            events.add(new TimelineEvent("VALIDATING", operation.getStartedAt(), "Validating repair patch"));
        }
        if (operation.getCompletedAt() != null) {
            String detail = operation.getStatus().name();
            if (operation.getErrorCode() != null) {
                detail = detail + " " + operation.getErrorCode();
            }
            events.add(new TimelineEvent("COMPLETED", operation.getCompletedAt(), detail));
        }
        return List.copyOf(events);
    }
}
