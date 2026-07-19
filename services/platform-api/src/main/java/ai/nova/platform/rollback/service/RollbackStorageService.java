package ai.nova.platform.rollback.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.rollback.dto.RollbackDtos.PlanView;
import ai.nova.platform.rollback.dto.RollbackDtos.Rollback;
import ai.nova.platform.rollback.dto.RollbackDtos.TargetRef;
import ai.nova.platform.rollback.dto.RollbackDtos.TimelineEvent;
import ai.nova.platform.rollback.dto.RollbackDtos.ValidationCheck;
import ai.nova.platform.rollback.entity.RollbackEventEntity;
import ai.nova.platform.rollback.entity.RollbackEventType;
import ai.nova.platform.rollback.entity.RollbackOperationEntity;
import ai.nova.platform.rollback.entity.RollbackPlanEntity;
import ai.nova.platform.rollback.entity.RollbackRiskLevel;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.entity.RollbackStrategy;
import ai.nova.platform.rollback.entity.RollbackTargetEntity;
import ai.nova.platform.rollback.entity.RollbackValidationEntity;
import ai.nova.platform.rollback.entity.RollbackValidationResult;
import ai.nova.platform.rollback.repository.RollbackEventRepository;
import ai.nova.platform.rollback.repository.RollbackOperationRepository;
import ai.nova.platform.rollback.repository.RollbackPlanRepository;
import ai.nova.platform.rollback.repository.RollbackTargetRepository;
import ai.nova.platform.rollback.repository.RollbackValidationRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class RollbackStorageService {

    private final RollbackOperationRepository operationRepository;
    private final RollbackPlanRepository planRepository;
    private final RollbackTargetRepository targetRepository;
    private final RollbackEventRepository eventRepository;
    private final RollbackValidationRepository validationRepository;

    public RollbackStorageService(
            RollbackOperationRepository operationRepository,
            RollbackPlanRepository planRepository,
            RollbackTargetRepository targetRepository,
            RollbackEventRepository eventRepository,
            RollbackValidationRepository validationRepository) {
        this.operationRepository = operationRepository;
        this.planRepository = planRepository;
        this.targetRepository = targetRepository;
        this.eventRepository = eventRepository;
        this.validationRepository = validationRepository;
    }

    @Transactional
    public RollbackOperationEntity createDraft(
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID deploymentId,
            UUID targetReleaseId,
            String currentVersion,
            String targetVersion,
            UUID environmentId,
            String environmentCode,
            RollbackStrategy strategy,
            String reason,
            RollbackRiskLevel riskLevel,
            String planJson,
            String planHash,
            UUID createdBy) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        RollbackOperationEntity entity = new RollbackOperationEntity(
                id,
                organizationId,
                projectId,
                releaseId,
                deploymentId,
                targetReleaseId,
                currentVersion,
                targetVersion,
                environmentId,
                environmentCode,
                RollbackStatus.DRAFT,
                strategy,
                planHash,
                createdBy,
                now);
        operationRepository.save(entity);

        planRepository.save(new RollbackPlanEntity(
                UUID.randomUUID(),
                id,
                releaseId,
                targetReleaseId,
                deploymentId,
                environmentCode,
                strategy,
                truncate(reason, 2000),
                riskLevel,
                RollbackValidationResult.PENDING,
                planJson,
                false,
                now));

        targetRepository.save(new RollbackTargetEntity(
                UUID.randomUUID(), id, targetReleaseId, targetVersion, 0, now));

        appendEvent(id, RollbackEventType.CREATED, "Rollback plan created", now);
        return entity;
    }

    @Transactional
    public void appendEvent(UUID rollbackId, RollbackEventType type, String detail, Instant at) {
        eventRepository.save(
                new RollbackEventEntity(UUID.randomUUID(), rollbackId, type, truncate(detail, 2000), at));
    }

    @Transactional
    public void appendValidation(UUID rollbackId, String checkCode, boolean passed, String message, Instant at) {
        validationRepository.save(new RollbackValidationEntity(
                UUID.randomUUID(), rollbackId, checkCode, passed, truncate(message, 2000), at));
    }

    public RollbackOperationEntity require(UUID id) {
        return operationRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROLLBACK_NOT_FOUND", "Rollback not found"));
    }

    public RollbackOperationEntity requireForOrg(UUID id, UUID organizationId) {
        return operationRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROLLBACK_NOT_FOUND", "Rollback not found"));
    }

    public RollbackPlanEntity requirePlan(UUID rollbackId) {
        return planRepository
                .findByRollbackOperationId(rollbackId)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "ROLLBACK_NOT_FOUND", "Rollback plan not found"));
    }

    public Rollback toDto(RollbackOperationEntity entity) {
        RollbackPlanEntity plan = requirePlan(entity.getId());
        List<RollbackTargetEntity> targets =
                targetRepository.findByRollbackOperationIdOrderBySortOrderAsc(entity.getId());
        List<RollbackValidationEntity> validations =
                validationRepository.findByRollbackOperationIdOrderByCreatedAtAsc(entity.getId());
        List<RollbackEventEntity> events =
                eventRepository.findByRollbackOperationIdOrderByCreatedAtAsc(entity.getId());

        PlanView planView = new PlanView(
                plan.getId(),
                plan.getCurrentReleaseOperationId(),
                plan.getTargetReleaseOperationId(),
                plan.getDeploymentOperationId(),
                plan.getEnvironmentCode(),
                plan.getStrategy(),
                plan.getReason(),
                plan.getRiskLevel(),
                plan.getValidationResult(),
                plan.getValidationMessage(),
                plan.isImmutable(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());

        List<TargetRef> targetRefs = new ArrayList<>();
        for (RollbackTargetEntity t : targets) {
            targetRefs.add(new TargetRef(
                    t.getId(), t.getTargetReleaseOperationId(), t.getTargetVersion(), t.getSortOrder(), t.getCreatedAt()));
        }
        List<ValidationCheck> checks = new ArrayList<>();
        for (RollbackValidationEntity v : validations) {
            checks.add(new ValidationCheck(v.getId(), v.getCheckCode(), v.isPassed(), v.getMessage(), v.getCreatedAt()));
        }
        List<TimelineEvent> timeline = events.stream()
                .map(e -> new TimelineEvent(e.getEventType().name(), e.getCreatedAt(), e.getDetail()))
                .toList();

        return new Rollback(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getReleaseOperationId(),
                entity.getDeploymentOperationId(),
                entity.getTargetReleaseOperationId(),
                entity.getCurrentVersion(),
                entity.getTargetVersion(),
                entity.getEnvironmentId(),
                entity.getEnvironmentCode(),
                entity.getStatus(),
                entity.getStrategy(),
                entity.getRollbackPlanHash(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getValidatedAt(),
                entity.getUpdatedAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                planView,
                targetRefs,
                checks,
                timeline);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
