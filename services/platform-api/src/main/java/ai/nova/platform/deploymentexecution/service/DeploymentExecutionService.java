package ai.nova.platform.deploymentexecution.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
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
import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
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
    private final ExecutionTransitionService transitionService;
    private final DeploymentExecutionWorker worker;
    private final ExecutorService deploymentExecutionExecutor;

    public DeploymentExecutionService(
            ExecutionProperties properties,
            DeploymentExecutionAuthorizationService authorizationService,
            ExecutionStorageService storageService,
            ExecutionValidationService validationService,
            DeploymentExecutionRepository executionRepository,
            DeploymentExecutionProviderRegistry providerRegistry,
            AuditRecordingSupport auditRecordingSupport,
            ExecutionTransitionService transitionService,
            DeploymentExecutionWorker worker,
            @Qualifier("deploymentExecutionExecutor") ExecutorService deploymentExecutionExecutor) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.validationService = validationService;
        this.executionRepository = executionRepository;
        this.providerRegistry = providerRegistry;
        this.auditRecordingSupport = auditRecordingSupport;
        this.transitionService = transitionService;
        this.worker = worker;
        this.deploymentExecutionExecutor = deploymentExecutionExecutor;
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

        return queueAfterValidation(request, user, provider, fingerprint, outcome);
    }

    private DeploymentExecution queueAfterValidation(
            CreateExecutionRequest request,
            AuthenticatedUser user,
            ExecutionProviderCode provider,
            String fingerprint,
            ValidationOutcome outcome) {
        try {
            DeploymentExecutionEntity created = storageService.createQueuedIsolated(
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
        } catch (DataIntegrityViolationException ex) {
            var raced = executionRepository.findByOrganizationIdAndExecutionFingerprint(
                    user.getOrganizationId(), fingerprint);
            if (raced.isPresent()) {
                return storageService.toDto(raced.get());
            }
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_CONCURRENCY_BLOCKED",
                    "Another active deployment execution exists for this environment");
        }
    }

    /**
     * Atomically claims QUEUED→STARTING and dispatches provider work to a managed executor.
     * Returns immediately with STARTING; does not hold a DB transaction during provider I/O.
     */
    public DeploymentExecution start(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentExecutionAuthorizationService.EXECUTION_RUN);
        requireEnabled();

        DeploymentExecutionEntity entity = storageService.requireForOrg(id, user.getOrganizationId());
        if (entity.getStatus() != ExecutionStatus.QUEUED) {
            if (entity.getStatus() == ExecutionStatus.STARTING
                    || entity.getStatus() == ExecutionStatus.DEPLOYING
                    || entity.getStatus() == ExecutionStatus.VERIFYING
                    || entity.getStatus() == ExecutionStatus.COMPLETED) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EXECUTION_ALREADY_STARTED",
                        "Execution was already claimed or started");
            }
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_INVALID_STATUS",
                    "Execution must be QUEUED to start; was " + entity.getStatus());
        }
        if (entity.isCancelRequested()) {
            transitionService.finalizeCancelled(id, user);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_INVALID_STATUS",
                    "Execution was cancelled before start");
        }

        boolean claimed = transitionService.claimQueued(id, user.getOrganizationId(), user);
        if (!claimed) {
            DeploymentExecutionEntity latest = storageService.requireForOrg(id, user.getOrganizationId());
            if (latest.isCancelRequested() || latest.getStatus() == ExecutionStatus.CANCELLED) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EXECUTION_INVALID_STATUS",
                        "Execution was cancelled before start");
            }
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EXECUTION_ALREADY_STARTED",
                    "Execution was already claimed by another start request");
        }

        try {
            deploymentExecutionExecutor.execute(() -> worker.run(id, user));
        } catch (RejectedExecutionException ex) {
            transitionService.failIfActive(
                    id, "EXECUTION_QUEUE_FULL", "Deployment execution worker queue is full", user);
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EXECUTION_QUEUE_FULL",
                    "Deployment execution worker queue is full");
        }

        return storageService.toDto(storageService.requireForOrg(id, user.getOrganizationId()));
    }

    /**
     * Marks cancellation requested atomically, invokes provider.cancel (cooperative), and finalizes
     * CANCELLED when the cancel path wins the race against COMPLETED.
     */
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

        boolean marked = transitionService.requestCancel(id, user.getOrganizationId());
        if (!marked && !entity.isCancelRequested()) {
            DeploymentExecutionEntity latest = storageService.requireForOrg(id, user.getOrganizationId());
            if (latest.getStatus() == ExecutionStatus.COMPLETED
                    || latest.getStatus() == ExecutionStatus.FAILED
                    || latest.getStatus() == ExecutionStatus.CANCELLED) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "EXECUTION_INVALID_STATUS",
                        "Cannot cancel execution in status " + latest.getStatus());
            }
        }

        entity = storageService.requireForOrg(id, user.getOrganizationId());
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
            storageService.appendLog(
                    id,
                    ExecutionLogLevel.WARN,
                    "Provider cancel reported: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }

        storageService.appendLog(
                id,
                ExecutionLogLevel.WARN,
                "Cancellation requested; provider cancellation may be cooperative");

        transitionService.finalizeCancelled(id, user);
        return storageService.toDto(storageService.requireForOrg(id, user.getOrganizationId()));
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
}
