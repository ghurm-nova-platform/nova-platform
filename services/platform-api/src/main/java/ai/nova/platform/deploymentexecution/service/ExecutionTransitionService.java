package ai.nova.platform.deploymentexecution.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity;
import ai.nova.platform.deploymentexecution.entity.ExecutionEventType;
import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionRepository;
import ai.nova.platform.security.AuthenticatedUser;

/**
 * Short, independent transactions for execution state transitions.
 * Provider I/O must never run inside these methods.
 */
@Service
public class ExecutionTransitionService {

    public static final EnumSet<ExecutionStatus> ACTIVE_STATUSES = EnumSet.of(
            ExecutionStatus.READY,
            ExecutionStatus.QUEUED,
            ExecutionStatus.STARTING,
            ExecutionStatus.DEPLOYING,
            ExecutionStatus.VERIFYING);

    private final DeploymentExecutionRepository executionRepository;
    private final ExecutionStorageService storageService;
    private final AuditRecordingSupport auditRecordingSupport;

    public ExecutionTransitionService(
            DeploymentExecutionRepository executionRepository,
            ExecutionStorageService storageService,
            AuditRecordingSupport auditRecordingSupport) {
        this.executionRepository = executionRepository;
        this.storageService = storageService;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimQueued(UUID id, UUID organizationId, AuthenticatedUser user) {
        Instant now = Instant.now();
        int updated = executionRepository.claimQueued(
                id, organizationId, ExecutionStatus.QUEUED, ExecutionStatus.STARTING, now);
        if (updated == 0) {
            return false;
        }
        storageService.appendEvent(id, ExecutionEventType.STARTING, "Execution claimed for start", now);
        DeploymentExecutionEntity entity = executionRepository.findById(id).orElseThrow();
        audit(user, entity, AuditAction.START, AuditResult.SUCCESS, Map.of("provider", entity.getProvider().name()));
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean transition(
            UUID id,
            ExecutionStatus expected,
            ExecutionStatus next,
            ExecutionEventType eventType,
            String step,
            String stage,
            String detail,
            AuthenticatedUser user,
            AuditAction auditAction) {
        Instant now = Instant.now();
        int updated = executionRepository.transitionIfActive(id, expected, next, step, stage, now);
        if (updated == 0) {
            return false;
        }
        storageService.appendEvent(id, eventType, detail, now);
        if (auditAction != null && user != null) {
            DeploymentExecutionEntity entity = executionRepository.findById(id).orElseThrow();
            audit(user, entity, auditAction, AuditResult.SUCCESS, Map.of());
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean completeIfNotCancelled(UUID id, AuthenticatedUser user) {
        Instant now = Instant.now();
        int updated = executionRepository.completeIfNotCancelled(
                id, ExecutionStatus.VERIFYING, ExecutionStatus.COMPLETED, now);
        if (updated == 0) {
            return false;
        }
        DeploymentExecutionEntity entity = executionRepository.findById(id).orElseThrow();
        entity.setDurationMs(ExecutionStorageService.computeDuration(entity.getStartedAt(), now));
        executionRepository.save(entity);
        storageService.appendEvent(id, ExecutionEventType.COMPLETED, "Execution completed", now);
        audit(user, entity, AuditAction.COMPLETE, AuditResult.SUCCESS, Map.of());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean failIfActive(UUID id, String errorCode, String errorMessage, AuthenticatedUser user) {
        Instant now = Instant.now();
        String code = truncate(errorCode, 80);
        String message = truncate(errorMessage, 2000);
        int updated = executionRepository.failIfActive(
                id, ExecutionStatus.FAILED, ACTIVE_STATUSES, code, message, now);
        if (updated == 0) {
            return false;
        }
        DeploymentExecutionEntity entity = executionRepository.findById(id).orElseThrow();
        entity.setDurationMs(ExecutionStorageService.computeDuration(entity.getStartedAt(), now));
        executionRepository.save(entity);
        storageService.appendEvent(id, ExecutionEventType.FAILED, message, now);
        storageService.appendLog(id, ExecutionLogLevel.ERROR, message);
        audit(user, entity, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", code == null ? "EXECUTION_FAILED" : code));
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean requestCancel(UUID id, UUID organizationId) {
        Instant now = Instant.now();
        return executionRepository.markCancelRequested(id, organizationId, ACTIVE_STATUSES, now) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finalizeCancelled(UUID id, AuthenticatedUser user) {
        Instant now = Instant.now();
        int updated = executionRepository.finalizeCancelled(
                id, ExecutionStatus.CANCELLED, ACTIVE_STATUSES, now);
        if (updated == 0) {
            return false;
        }
        DeploymentExecutionEntity entity = executionRepository.findById(id).orElseThrow();
        entity.setDurationMs(ExecutionStorageService.computeDuration(entity.getStartedAt(), now));
        executionRepository.save(entity);
        storageService.appendEvent(id, ExecutionEventType.CANCELLED, "Execution cancelled", now);
        audit(user, entity, AuditAction.CANCEL, AuditResult.SUCCESS, Map.of());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isCancelRequested(UUID id) {
        return executionRepository.findById(id).map(DeploymentExecutionEntity::isCancelRequested).orElse(false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public DeploymentExecutionEntity require(UUID id) {
        return executionRepository.findById(id).orElseThrow();
    }

    private void audit(
            AuthenticatedUser user,
            DeploymentExecutionEntity entity,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        if (user == null) {
            return;
        }
        auditRecordingSupport.recordDomainEvent(
                user,
                entity.getProjectId(),
                AuditEntityType.DEPLOYMENT,
                entity.getId(),
                entity.getProvider().name() + ":" + entity.getStatus(),
                action,
                result,
                AuditSource.DEPLOYMENT_EXECUTION,
                details);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
