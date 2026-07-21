package ai.nova.platform.deploymentexecution.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.ArtifactView;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.DeploymentExecution;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.LogEntry;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.ResultView;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.StepView;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.TimelineEvent;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.ValidationCheck;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionArtifactEntity;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEventEntity;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionLogEntity;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionResultEntity;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionStepEntity;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionValidationEntity;
import ai.nova.platform.deploymentexecution.entity.ExecutionEventType;
import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.entity.ExecutionStepStatus;
import ai.nova.platform.deploymentexecution.provider.ExecutionStorageCallbacks;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionArtifactRepository;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionEventRepository;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionLogRepository;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionRepository;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionResultRepository;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionStepRepository;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionValidationRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ExecutionStorageService implements ExecutionStorageCallbacks {

    private final DeploymentExecutionRepository executionRepository;
    private final DeploymentExecutionStepRepository stepRepository;
    private final DeploymentExecutionLogRepository logRepository;
    private final DeploymentExecutionResultRepository resultRepository;
    private final DeploymentExecutionArtifactRepository artifactRepository;
    private final DeploymentExecutionValidationRepository validationRepository;
    private final DeploymentExecutionEventRepository eventRepository;

    public ExecutionStorageService(
            DeploymentExecutionRepository executionRepository,
            DeploymentExecutionStepRepository stepRepository,
            DeploymentExecutionLogRepository logRepository,
            DeploymentExecutionResultRepository resultRepository,
            DeploymentExecutionArtifactRepository artifactRepository,
            DeploymentExecutionValidationRepository validationRepository,
            DeploymentExecutionEventRepository eventRepository) {
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.logRepository = logRepository;
        this.resultRepository = resultRepository;
        this.artifactRepository = artifactRepository;
        this.validationRepository = validationRepository;
        this.eventRepository = eventRepository;
    }

    public static String executionFingerprint(
            UUID organizationId, UUID releaseId, UUID environmentId, ExecutionProviderCode provider) {
        String payload = organizationId + "|" + releaseId + "|" + environmentId + "|" + provider.name();
        return sha256(payload);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeploymentExecutionEntity createQueuedIsolated(
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID environmentId,
            UUID deploymentObservationId,
            ExecutionProviderCode provider,
            String releaseManifestHash,
            String releaseContentFingerprint,
            String executionFingerprint,
            UUID triggeredBy) {
        return createQueued(
                organizationId,
                projectId,
                releaseId,
                environmentId,
                deploymentObservationId,
                provider,
                releaseManifestHash,
                releaseContentFingerprint,
                executionFingerprint,
                triggeredBy);
    }

    @Transactional
    public DeploymentExecutionEntity createQueued(
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID environmentId,
            UUID deploymentObservationId,
            ExecutionProviderCode provider,
            String releaseManifestHash,
            String releaseContentFingerprint,
            String executionFingerprint,
            UUID triggeredBy) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        DeploymentExecutionEntity entity = new DeploymentExecutionEntity(
                id,
                organizationId,
                projectId,
                releaseId,
                environmentId,
                deploymentObservationId,
                provider,
                ExecutionStatus.QUEUED,
                releaseManifestHash,
                releaseContentFingerprint,
                executionFingerprint,
                triggeredBy,
                now);
        entity.setActiveEnvironmentSlot(environmentId);
        executionRepository.saveAndFlush(entity);
        appendEvent(id, ExecutionEventType.CREATED, "Deployment execution created", now);
        appendEvent(id, ExecutionEventType.QUEUED, "Queued for execution", now);
        seedSteps(id, now);
        return entity;
    }

    @Transactional
    public DeploymentExecutionEntity createFailed(
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID environmentId,
            UUID deploymentObservationId,
            ExecutionProviderCode provider,
            String releaseManifestHash,
            String releaseContentFingerprint,
            String executionFingerprint,
            UUID triggeredBy,
            String errorCode,
            String errorMessage) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        DeploymentExecutionEntity entity = new DeploymentExecutionEntity(
                id,
                organizationId,
                projectId,
                releaseId,
                environmentId,
                deploymentObservationId,
                provider,
                ExecutionStatus.FAILED,
                releaseManifestHash,
                releaseContentFingerprint,
                executionFingerprint,
                triggeredBy,
                now);
        entity.setErrorCode(errorCode);
        entity.setErrorMessage(truncate(errorMessage, 2000));
        entity.setFinishedAt(now);
        entity.setDurationMs(0L);
        executionRepository.save(entity);
        appendEvent(id, ExecutionEventType.CREATED, "Deployment execution created", now);
        appendEvent(id, ExecutionEventType.VALIDATION_FAILED, truncate(errorMessage, 2000), now);
        appendEvent(id, ExecutionEventType.FAILED, truncate(errorMessage, 2000), now);
        return entity;
    }

    private void seedSteps(UUID executionId, Instant now) {
        stepRepository.save(new DeploymentExecutionStepEntity(
                UUID.randomUUID(), executionId, "prepare", "PREPARE", ExecutionStepStatus.PENDING, 0, null, null, null));
        stepRepository.save(new DeploymentExecutionStepEntity(
                UUID.randomUUID(), executionId, "deploy", "DEPLOY", ExecutionStepStatus.PENDING, 1, null, null, null));
        stepRepository.save(new DeploymentExecutionStepEntity(
                UUID.randomUUID(), executionId, "verify", "VERIFY", ExecutionStepStatus.PENDING, 2, null, null, null));
    }

    @Transactional
    public void appendEvent(UUID executionId, ExecutionEventType type, String detail, Instant at) {
        eventRepository.save(new DeploymentExecutionEventEntity(
                UUID.randomUUID(), executionId, type, truncate(detail, 2000), at));
    }

    @Transactional
    public void appendValidation(UUID executionId, String checkCode, boolean passed, String message, Instant at) {
        validationRepository.save(new DeploymentExecutionValidationEntity(
                UUID.randomUUID(), executionId, checkCode, passed, truncate(message, 2000), at));
    }

    @Override
    @Transactional
    public void appendLog(UUID executionId, ExecutionLogLevel level, String message) {
        logRepository.save(new DeploymentExecutionLogEntity(
                UUID.randomUUID(), executionId, level, truncate(message, 4000), Instant.now()));
    }

    @Override
    @Transactional
    public void saveResult(UUID executionId, boolean success, String summary, String providerResponseJson) {
        resultRepository
                .findByExecutionId(executionId)
                .ifPresentOrElse(
                        existing -> {
                            // append-only results table — ignore duplicate writes
                        },
                        () -> resultRepository.save(new DeploymentExecutionResultEntity(
                                UUID.randomUUID(),
                                executionId,
                                success,
                                truncate(summary, 2000),
                                providerResponseJson,
                                Instant.now())));
    }

    @Override
    @Transactional
    public void saveArtifact(
            UUID executionId, String artifactType, String name, String contentRef, String checksum) {
        artifactRepository.save(new DeploymentExecutionArtifactEntity(
                UUID.randomUUID(),
                executionId,
                truncate(artifactType, 60),
                truncate(name, 200),
                truncate(contentRef, 500),
                truncate(checksum, 128),
                Instant.now()));
    }

    @Override
    @Transactional
    public void updateStep(UUID executionId, String stepKey, String stage, String detail) {
        Instant now = Instant.now();
        stepRepository.findByExecutionIdOrderBySortOrderAsc(executionId).stream()
                .filter(step -> step.getStepKey().equals(stepKey))
                .findFirst()
                .ifPresent(step -> {
                    step.setStatus(ExecutionStepStatus.RUNNING);
                    step.setDetail(truncate(detail, 2000));
                    if (step.getStartedAt() == null) {
                        step.setStartedAt(now);
                    }
                    stepRepository.save(step);
                });
        executionRepository.findById(executionId).ifPresent(entity -> {
            entity.setCurrentStep(stepKey);
            entity.setCurrentStage(stage);
            entity.setUpdatedAt(now);
            executionRepository.save(entity);
        });
    }

    @Transactional
    public void completeStep(UUID executionId, String stepKey, boolean success, String detail) {
        Instant now = Instant.now();
        stepRepository.findByExecutionIdOrderBySortOrderAsc(executionId).stream()
                .filter(step -> step.getStepKey().equals(stepKey))
                .findFirst()
                .ifPresent(step -> {
                    step.setStatus(success ? ExecutionStepStatus.COMPLETED : ExecutionStepStatus.FAILED);
                    step.setDetail(truncate(detail, 2000));
                    step.setFinishedAt(now);
                    if (step.getStartedAt() == null) {
                        step.setStartedAt(now);
                    }
                    stepRepository.save(step);
                });
    }

    public DeploymentExecutionEntity requireForOrg(UUID id, UUID organizationId) {
        return executionRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "DEPLOYMENT_EXECUTION_NOT_FOUND", "Deployment execution not found"));
    }

    public DeploymentExecution toDto(DeploymentExecutionEntity entity) {
        UUID id = entity.getId();
        List<ValidationCheck> validations = validationRepository.findByExecutionIdOrderByCreatedAtAsc(id).stream()
                .map(v -> new ValidationCheck(v.getId(), v.getCheckCode(), v.isPassed(), v.getMessage(), v.getCreatedAt()))
                .toList();
        List<StepView> steps = stepRepository.findByExecutionIdOrderBySortOrderAsc(id).stream()
                .map(s -> new StepView(
                        s.getId(),
                        s.getStepKey(),
                        s.getStage(),
                        s.getStatus(),
                        s.getSortOrder(),
                        s.getDetail(),
                        s.getStartedAt(),
                        s.getFinishedAt()))
                .toList();
        List<ArtifactView> artifacts = artifactRepository.findByExecutionIdOrderByCreatedAtAsc(id).stream()
                .map(a -> new ArtifactView(
                        a.getId(), a.getArtifactType(), a.getName(), a.getContentRef(), a.getChecksum(), a.getCreatedAt()))
                .toList();
        ResultView result = resultRepository.findByExecutionId(id).map(r -> new ResultView(
                        r.getId(), r.isSuccess(), r.getSummary(), r.getProviderResponseJson(), r.getCreatedAt()))
                .orElse(null);
        List<TimelineEvent> timeline = eventRepository.findByExecutionIdOrderByCreatedAtAsc(id).stream()
                .map(e -> new TimelineEvent(e.getEventType().name(), e.getCreatedAt(), e.getDetail()))
                .toList();
        return new DeploymentExecution(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getReleaseOperationId(),
                entity.getEnvironmentId(),
                entity.getDeploymentObservationId(),
                entity.getProvider(),
                entity.getStatus(),
                entity.getCurrentStep(),
                entity.getCurrentStage(),
                entity.getReleaseManifestHash(),
                entity.getReleaseContentFingerprint(),
                entity.getExecutionFingerprint(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getDurationMs(),
                entity.getTriggeredBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                validations,
                steps,
                artifacts,
                result,
                timeline);
    }

    public List<LogEntry> logs(UUID executionId) {
        return logRepository.findByExecutionIdOrderByCreatedAtAsc(executionId).stream()
                .map(l -> new LogEntry(l.getId(), l.getLevel(), l.getMessage(), l.getCreatedAt()))
                .toList();
    }

    public static Long computeDuration(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
