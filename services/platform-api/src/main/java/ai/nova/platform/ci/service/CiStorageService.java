package ai.nova.platform.ci.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.ci.dto.CiDtos.CiFailureSummary;
import ai.nova.platform.ci.dto.CiDtos.CiJob;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.dto.CiDtos.CiStep;
import ai.nova.platform.ci.dto.CiDtos.CiWorkflowRun;
import ai.nova.platform.ci.dto.CiDtos.TimelineEvent;
import ai.nova.platform.ci.entity.CiJobEntity;
import ai.nova.platform.ci.entity.CiObservationOperationEntity;
import ai.nova.platform.ci.entity.CiObservationStatus;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.entity.CiStepEntity;
import ai.nova.platform.ci.entity.CiWorkflowRunEntity;
import ai.nova.platform.ci.provider.ProviderJob;
import ai.nova.platform.ci.provider.ProviderStep;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;
import ai.nova.platform.ci.repository.CiJobRepository;
import ai.nova.platform.ci.repository.CiObservationOperationRepository;
import ai.nova.platform.ci.repository.CiStepRepository;
import ai.nova.platform.ci.repository.CiWorkflowRunRepository;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService.ResolvedRepositoryConfig;
import ai.nova.platform.web.error.ApiException;

@Service
public class CiStorageService {

    private final CiObservationOperationRepository operationRepository;
    private final CiWorkflowRunRepository workflowRunRepository;
    private final CiJobRepository jobRepository;
    private final CiStepRepository stepRepository;

    public CiStorageService(
            CiObservationOperationRepository operationRepository,
            CiWorkflowRunRepository workflowRunRepository,
            CiJobRepository jobRepository,
            CiStepRepository stepRepository) {
        this.operationRepository = operationRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.jobRepository = jobRepository;
        this.stepRepository = stepRepository;
    }

    @Transactional
    public CiObservationOperation startPending(
            UUID operationId,
            AgentOrchestrationTask task,
            PullRequestOperation pullRequestOperation,
            ResolvedRepositoryConfig config,
            Instant startedAt,
            List<TimelineEvent> timeline) {
        Instant now = Instant.now();
        String commitHash = pullRequestOperation.remoteCommitHash() != null
                ? pullRequestOperation.remoteCommitHash()
                : pullRequestOperation.localCommitHash();
        CiObservationOperationEntity operation = new CiObservationOperationEntity(
                operationId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getId(),
                pullRequestOperation.id(),
                CiObservationStatus.PENDING,
                config.effectiveProvider(),
                config.repositoryRef().owner(),
                config.repositoryRef().name(),
                pullRequestOperation.sourceBranch(),
                pullRequestOperation.targetBranch(),
                commitHash,
                pullRequestOperation.pullRequestNumber(),
                CiOverallStatus.UNKNOWN,
                null,
                null,
                null,
                null,
                startedAt,
                null,
                now,
                now);
        operationRepository.save(operation);
        return toOperation(operation, List.of(), timeline == null ? List.of() : timeline, null);
    }

    @Transactional
    public CiObservationOperation updateStatus(
            UUID operationId, CiObservationStatus status, List<TimelineEvent> timeline) {
        CiObservationOperationEntity operation = requireOperation(operationId);
        operation.updateStatus(status);
        operationRepository.save(operation);
        return reloadOperation(operation.getId(), timeline, null);
    }

    @Transactional
    public CiObservationOperation markSucceeded(
            UUID operationId,
            CiOverallStatus overallStatus,
            CiFailureSummary failureSummary,
            String retryRecommendation,
            String failureSummaryText,
            List<ProviderWorkflowRun> runs,
            List<List<ProviderJob>> jobsPerRun,
            Instant completedAt,
            List<TimelineEvent> timeline) {
        CiObservationOperationEntity operation = requireOperation(operationId);
        operation.markSucceeded(overallStatus, failureSummaryText, retryRecommendation, completedAt);
        operationRepository.save(operation);

        Instant now = Instant.now();
        for (int i = 0; i < runs.size(); i++) {
            ProviderWorkflowRun run = runs.get(i);
            UUID runId = UUID.randomUUID();
            CiWorkflowRunEntity runEntity = new CiWorkflowRunEntity(
                    runId,
                    operationId,
                    run.externalWorkflowId(),
                    run.workflowName(),
                    run.externalRunId(),
                    run.runUrl(),
                    run.status(),
                    run.conclusion(),
                    run.durationMs(),
                    run.triggerEvent(),
                    run.commitHash(),
                    run.branch(),
                    run.pullRequestNumber(),
                    run.failureReason(),
                    run.startedAt(),
                    run.completedAt(),
                    now);
            workflowRunRepository.save(runEntity);

            List<ProviderJob> jobs = i < jobsPerRun.size() ? jobsPerRun.get(i) : List.of();
            for (ProviderJob job : jobs) {
                UUID jobId = UUID.randomUUID();
                CiJobEntity jobEntity = new CiJobEntity(
                        jobId,
                        runId,
                        job.externalJobId(),
                        job.jobName(),
                        job.status(),
                        job.conclusion(),
                        job.durationMs(),
                        job.failureReason(),
                        job.startedAt(),
                        job.completedAt(),
                        now);
                jobRepository.save(jobEntity);

                for (ProviderStep step : job.steps()) {
                    CiStepEntity stepEntity = new CiStepEntity(
                            UUID.randomUUID(),
                            jobId,
                            step.stepNumber(),
                            step.stepName(),
                            step.status(),
                            step.conclusion(),
                            step.durationMs(),
                            step.failureReason(),
                            step.startedAt(),
                            step.completedAt(),
                            now);
                    stepRepository.save(stepEntity);
                }
            }
        }

        return reloadOperation(operationId, timeline, failureSummary);
    }

    @Transactional
    public CiObservationOperation markFailed(
            UUID operationId, String errorCode, String message, Instant completedAt, List<TimelineEvent> timeline) {
        CiObservationOperationEntity operation = requireOperation(operationId);
        operation.markFailed(errorCode, message, completedAt);
        operationRepository.save(operation);
        return reloadOperation(operationId, timeline, null);
    }

    @Transactional(readOnly = true)
    public CiObservationOperation findLatest(UUID taskId, UUID organizationId) {
        return operationRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(operation -> reloadOperation(operation.getId(), null, null))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CiObservationOperation> findHistory(UUID taskId, UUID organizationId) {
        return operationRepository.findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId).stream()
                .map(operation -> reloadOperation(operation.getId(), null, null))
                .toList();
    }

    private CiObservationOperation reloadOperation(
            UUID operationId, List<TimelineEvent> timelineOverride, CiFailureSummary failureSummaryOverride) {
        CiObservationOperationEntity operation = requireOperation(operationId);
        List<CiWorkflowRunEntity> runEntities =
                workflowRunRepository.findByCiObservationOperationIdOrderByCreatedAtAsc(operationId);
        List<CiWorkflowRun> workflows = new ArrayList<>();
        for (CiWorkflowRunEntity runEntity : runEntities) {
            List<CiJobEntity> jobEntities =
                    jobRepository.findByCiWorkflowRunIdOrderByCreatedAtAsc(runEntity.getId());
            List<CiJob> jobs = new ArrayList<>();
            for (CiJobEntity jobEntity : jobEntities) {
                List<CiStepEntity> stepEntities =
                        stepRepository.findByCiJobIdOrderByStepNumberAsc(jobEntity.getId());
                List<CiStep> steps = stepEntities.stream().map(CiStorageService::toStep).toList();
                jobs.add(toJob(jobEntity, steps));
            }
            workflows.add(toWorkflowRun(runEntity, jobs));
        }

        CiFailureSummary failureSummary = failureSummaryOverride;
        if (failureSummary == null && operation.getFailureSummary() != null) {
            failureSummary = parseFailureSummaryStub(operation.getFailureSummary());
        }

        List<TimelineEvent> timeline =
                timelineOverride == null ? buildTimeline(operation, workflows) : timelineOverride;
        return toOperation(operation, workflows, timeline, failureSummary);
    }

    private CiObservationOperationEntity requireOperation(UUID operationId) {
        return operationRepository
                .findById(operationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CI_NOT_FOUND", "CI observation operation not found"));
    }

    private static CiObservationOperation toOperation(
            CiObservationOperationEntity operation,
            List<CiWorkflowRun> workflows,
            List<TimelineEvent> timeline,
            CiFailureSummary failureSummary) {
        return new CiObservationOperation(
                operation.getId(),
                operation.getTaskId(),
                operation.getProjectId(),
                operation.getPullRequestOperationId(),
                operation.getStatus(),
                operation.getProvider(),
                operation.getRepositoryOwner(),
                operation.getRepositoryName(),
                operation.getSourceBranch(),
                operation.getTargetBranch(),
                operation.getCommitHash(),
                operation.getPullRequestNumber(),
                operation.getOverallStatus(),
                failureSummary,
                operation.getRetryRecommendation(),
                workflows,
                operation.getErrorCode(),
                operation.getErrorMessage(),
                timeline,
                operation.getStartedAt(),
                operation.getCompletedAt(),
                operation.getCreatedAt());
    }

    private static CiWorkflowRun toWorkflowRun(CiWorkflowRunEntity entity, List<CiJob> jobs) {
        return new CiWorkflowRun(
                entity.getId(),
                entity.getExternalWorkflowId(),
                entity.getWorkflowName(),
                entity.getExternalRunId(),
                entity.getRunUrl(),
                entity.getStatus(),
                entity.getConclusion(),
                entity.getDurationMs(),
                entity.getTriggerEvent(),
                entity.getCommitHash(),
                entity.getBranch(),
                entity.getPullRequestNumber(),
                entity.getFailureReason(),
                jobs,
                entity.getStartedAt(),
                entity.getCompletedAt());
    }

    private static CiJob toJob(CiJobEntity entity, List<CiStep> steps) {
        return new CiJob(
                entity.getId(),
                entity.getExternalJobId(),
                entity.getJobName(),
                entity.getStatus(),
                entity.getConclusion(),
                entity.getDurationMs(),
                entity.getFailureReason(),
                steps,
                entity.getStartedAt(),
                entity.getCompletedAt());
    }

    private static CiStep toStep(CiStepEntity entity) {
        return new CiStep(
                entity.getId(),
                entity.getStepNumber(),
                entity.getStepName(),
                entity.getStatus(),
                entity.getConclusion(),
                entity.getDurationMs(),
                entity.getFailureReason(),
                entity.getStartedAt(),
                entity.getCompletedAt());
    }

    private static List<TimelineEvent> buildTimeline(
            CiObservationOperationEntity operation, List<CiWorkflowRun> workflows) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("STARTED", operation.getStartedAt(), "CI observation agent started"));
        if (operation.getStatus().ordinal() >= CiObservationStatus.FETCHING.ordinal()) {
            events.add(new TimelineEvent("FETCHING", operation.getStartedAt(), "Fetching workflow runs"));
        }
        if (operation.getStatus().ordinal() >= CiObservationStatus.PROCESSING.ordinal()) {
            events.add(new TimelineEvent(
                    "PROCESSING", operation.getStartedAt(), "Processing " + workflows.size() + " workflow runs"));
        }
        if (operation.getCompletedAt() != null) {
            String detail = operation.getStatus().name();
            if (operation.getErrorCode() != null) {
                detail = detail + " " + operation.getErrorCode();
            } else if (operation.getOverallStatus() != null) {
                detail = detail + " overall=" + operation.getOverallStatus().name();
            }
            events.add(new TimelineEvent("COMPLETED", operation.getCompletedAt(), detail));
        }
        return List.copyOf(events);
    }

    private static CiFailureSummary parseFailureSummaryStub(String text) {
        return new CiFailureSummary(0, 0, 0, 0, text == null ? List.of() : List.of(text), List.of());
    }
}
