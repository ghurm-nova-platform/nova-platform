package ai.nova.platform.deploymentexecution.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.deploymentexecution.config.ExecutionProperties;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.CreateExecutionRequest;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.DeploymentExecution;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.LogEntry;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity;
import ai.nova.platform.deploymentexecution.entity.ExecutionEventType;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.provider.DeploymentExecutionProvider;
import ai.nova.platform.deploymentexecution.provider.DeploymentExecutionProviderRegistry;
import ai.nova.platform.deploymentexecution.provider.ExecutionContext;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionRepository;
import ai.nova.platform.deploymentexecution.security.DeploymentExecutionAuthorizationService;
import ai.nova.platform.deploymentexecution.service.ExecutionValidationService.ValidationOutcome;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class DeploymentExecutionService {

    private final ExecutionProperties properties;
    private final DeploymentExecutionAuthorizationService authorizationService;
    private final ExecutionStorageService storageService;
    private final ExecutionValidationService validationService;
    private final DeploymentExecutionRepository executionRepository;
    private final DeploymentExecutionProviderRegistry providerRegistry;
    private final AuditRecordingSupport auditRecordingSupport;

    public DeploymentExecutionService(
            ExecutionProperties properties,
            DeploymentExecutionAuthorizationService authorizationService,
            ExecutionStorageService storageService,
            ExecutionValidationService validationService,
            DeploymentExecutionRepository executionRepository,
            DeploymentExecutionProviderRegistry providerRegistry,
            AuditRecordingSupport auditRecordingSupport) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.validationService = validationService;
        this.executionRepository = executionRepository;
        this.providerRegistry = providerRegistry;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional
    public DeploymentExecution create(CreateExecutionRequest request, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentExecutionAuthorizationService.EXECUTION_RUN);
        requireEnabled();

        ExecutionProviderCode provider = providerRegistry.resolveConfigured(properties.getProvider(), request.provider());
        String fingerprint = ExecutionStorageService.executionFingerprint(
                user.getOrganizationId(), request.releaseId(), request.environmentId(), provider);

        var existing = executionRepository.findByOrganizationIdAndExecutionFingerprint(
                user.getOrganizationId(), fingerprint);
        if (existing.isPresent()) {
            storageService.appendEvent(
                    existing.get().getId(),
                    ExecutionEventType.IDEMPOTENT_RETURN,
                    "Identical deployment execution already exists",
                    Instant.now());
            return storageService.toDto(existing.get());
        }

        ValidationOutcome outcome = validationService.validateForCreate(
                user.getOrganizationId(), request.releaseId(), request.environmentId(), provider);
        if (!outcome.passed()) {
            if (outcome.release() != null) {
                DeploymentExecutionEntity failed = storageService.createFailed(
                        user.getOrganizationId(),
                        outcome.release().getProjectId(),
                        request.releaseId(),
                        request.environmentId(),
                        request.deploymentObservationId(),
                        provider,
                        outcome.release().getManifestHash(),
                        outcome.release().getContentFingerprint(),
                        fingerprint,
                        user.getUserId(),
                        outcome.errorCode(),
                        outcome.message());
                validationService.persistChecks(failed.getId(), outcome.checks());
            }
            throw new ApiException(HttpStatus.CONFLICT, outcome.errorCode(), outcome.message());
        }

        DeploymentExecutionEntity created = storageService.createQueued(
                user.getOrganizationId(),
                outcome.release().getProjectId(),
                request.releaseId(),
                request.environmentId(),
                request.deploymentObservationId(),
                provider,
                outcome.release().getManifestHash(),
                outcome.release().getContentFingerprint(),
                fingerprint,
                user.getUserId());
        validationService.persistChecks(created.getId(), outcome.checks());
        audit(user, created, AuditAction.QUEUE, AuditResult.SUCCESS, Map.of("provider", provider.name()));
        return storageService.toDto(created);
    }

    @Transactional
    public DeploymentExecution start(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentExecutionAuthorizationService.EXECUTION_RUN);
        requireEnabled();

        DeploymentExecutionEntity entity = storageService.requireForOrg(id, user.getOrganizationId());
        if (entity.getStatus() != ExecutionStatus.QUEUED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_INVALID_STATUS",
                    "Execution must be QUEUED to start; was " + entity.getStatus());
        }

        Instant now = Instant.now();
        entity.setStatus(ExecutionStatus.STARTING);
        entity.setStartedAt(now);
        entity.setUpdatedAt(now);
        executionRepository.save(entity);
        storageService.appendEvent(id, ExecutionEventType.STARTING, "Execution starting", now);
        audit(user, entity, AuditAction.START, AuditResult.SUCCESS, Map.of("provider", entity.getProvider().name()));

        DeploymentExecutionProvider provider = providerRegistry.require(entity.getProvider());
        ExecutionContext context = new ExecutionContext(
                id,
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getReleaseOperationId(),
                entity.getEnvironmentId(),
                entity.getProvider(),
                null,
                storageService);

        try {
            provider.prepare(context);
            storageService.completeStep(id, "prepare", true, "Prepare completed");

            entity.setStatus(ExecutionStatus.DEPLOYING);
            entity.setUpdatedAt(Instant.now());
            executionRepository.save(entity);
            storageService.appendEvent(id, ExecutionEventType.DEPLOYING, "Deploying", Instant.now());

            provider.deploy(context);
            storageService.completeStep(id, "deploy", true, "Deploy completed");

            entity.setStatus(ExecutionStatus.VERIFYING);
            entity.setUpdatedAt(Instant.now());
            executionRepository.save(entity);
            storageService.appendEvent(id, ExecutionEventType.VERIFYING, "Verifying deployment", Instant.now());
            audit(user, entity, AuditAction.VERIFY, AuditResult.SUCCESS, Map.of());

            provider.verify(context);
            storageService.completeStep(id, "verify", true, "Verify completed");

            Instant finished = Instant.now();
            entity.setStatus(ExecutionStatus.COMPLETED);
            entity.setFinishedAt(finished);
            entity.setDurationMs(ExecutionStorageService.computeDuration(entity.getStartedAt(), finished));
            entity.setUpdatedAt(finished);
            executionRepository.save(entity);
            storageService.appendEvent(id, ExecutionEventType.COMPLETED, "Execution completed", finished);
            audit(user, entity, AuditAction.COMPLETE, AuditResult.SUCCESS, Map.of());
            return storageService.toDto(entity);
        } catch (Exception ex) {
            return failExecution(user, entity, ex);
        }
    }

    @Transactional
    public DeploymentExecution cancel(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentExecutionAuthorizationService.EXECUTION_RUN);
        requireEnabled();
        if (!properties.isAllowCancel()) {
            throw new ApiException(HttpStatus.CONFLICT, "EXECUTION_CANCEL_DISABLED", "Cancel is disabled");
        }

        DeploymentExecutionEntity entity = storageService.requireForOrg(id, user.getOrganizationId());
        if (entity.getStatus() == ExecutionStatus.COMPLETED
                || entity.getStatus() == ExecutionStatus.FAILED
                || entity.getStatus() == ExecutionStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_INVALID_STATUS",
                    "Cannot cancel execution in status " + entity.getStatus());
        }

        try {
            providerRegistry.require(entity.getProvider()).cancel(new ExecutionContext(
                    id,
                    entity.getOrganizationId(),
                    entity.getProjectId(),
                    entity.getReleaseOperationId(),
                    entity.getEnvironmentId(),
                    entity.getProvider(),
                    null,
                    storageService));
        } catch (Exception ex) {
            storageService.appendLog(id, ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel.WARN, ex.getMessage());
        }

        Instant now = Instant.now();
        entity.setStatus(ExecutionStatus.CANCELLED);
        entity.setFinishedAt(now);
        entity.setDurationMs(ExecutionStorageService.computeDuration(entity.getStartedAt(), now));
        entity.setUpdatedAt(now);
        executionRepository.save(entity);
        storageService.appendEvent(id, ExecutionEventType.CANCELLED, "Execution cancelled", now);
        audit(user, entity, AuditAction.CANCEL, AuditResult.SUCCESS, Map.of());
        return storageService.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<DeploymentExecution> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentExecutionAuthorizationService.EXECUTION_READ);
        requireEnabled();
        List<DeploymentExecutionEntity> entities = projectId == null
                ? executionRepository.findByOrganizationIdOrderByCreatedAtDesc(user.getOrganizationId())
                : executionRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        user.getOrganizationId(), projectId);
        return entities.stream().map(storageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DeploymentExecution get(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentExecutionAuthorizationService.EXECUTION_READ);
        requireEnabled();
        return storageService.toDto(storageService.requireForOrg(id, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public DeploymentExecution history(UUID id, AuthenticatedUser user) {
        return get(id, user);
    }

    @Transactional(readOnly = true)
    public List<LogEntry> logs(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentExecutionAuthorizationService.EXECUTION_READ);
        requireEnabled();
        storageService.requireForOrg(id, user.getOrganizationId());
        return storageService.logs(id);
    }

    private DeploymentExecution failExecution(
            AuthenticatedUser user, DeploymentExecutionEntity entity, Exception ex) {
        Instant now = Instant.now();
        String code = ex instanceof ApiException api ? api.getCode() : "EXECUTION_FAILED";
        String message = ex.getMessage() != null ? ex.getMessage() : "Execution failed";
        entity.setStatus(ExecutionStatus.FAILED);
        entity.setErrorCode(code);
        entity.setErrorMessage(truncate(message, 2000));
        entity.setFinishedAt(now);
        entity.setDurationMs(ExecutionStorageService.computeDuration(entity.getStartedAt(), now));
        entity.setUpdatedAt(now);
        executionRepository.save(entity);
        storageService.appendEvent(entity.getId(), ExecutionEventType.FAILED, message, now);
        storageService.appendLog(
                entity.getId(), ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel.ERROR, message);
        audit(user, entity, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", code));
        throw new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private void audit(
            AuthenticatedUser user,
            DeploymentExecutionEntity entity,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
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

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "EXECUTION_DISABLED", "Deployment Execution Engine is disabled");
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
