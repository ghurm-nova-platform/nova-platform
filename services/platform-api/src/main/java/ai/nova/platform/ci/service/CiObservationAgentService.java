package ai.nova.platform.ci.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.ci.config.CiObservationProperties;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.dto.CiDtos.CiRunRequest;
import ai.nova.platform.ci.dto.CiDtos.TimelineEvent;
import ai.nova.platform.ci.entity.CiObservationStatus;
import ai.nova.platform.ci.provider.CiProvider;
import ai.nova.platform.ci.provider.CiProviderRegistry;
import ai.nova.platform.ci.provider.ProviderJob;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;
import ai.nova.platform.ci.provider.WorkflowRunQuery;
import ai.nova.platform.ci.security.CiAuthorizationService;
import ai.nova.platform.ci.service.CiHealthAggregator.AggregatedHealth;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService.ResolvedRepositoryConfig;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * CI Observation Agent: read-only observation of CI workflow runs for successful pull requests.
 * Never reruns workflows, approves, merges, or deploys.
 */
@Service
public class CiObservationAgentService {

    private final CiAuthorizationService authorizationService;
    private final CiObservationProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final PullRequestStorageService pullRequestStorageService;
    private final ProjectRepositoryConfigService repositoryConfigService;
    private final CiProviderRegistry providerRegistry;
    private final CiHealthAggregator healthAggregator;
    private final CiStorageService storageService;

    public CiObservationAgentService(
            CiAuthorizationService authorizationService,
            CiObservationProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            ProjectRepository projectRepository,
            PullRequestStorageService pullRequestStorageService,
            ProjectRepositoryConfigService repositoryConfigService,
            CiProviderRegistry providerRegistry,
            CiHealthAggregator healthAggregator,
            CiStorageService storageService) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.pullRequestStorageService = pullRequestStorageService;
        this.repositoryConfigService = repositoryConfigService;
        this.providerRegistry = providerRegistry;
        this.healthAggregator = healthAggregator;
        this.storageService = storageService;
    }

    public CiObservationOperation run(CiRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, CiAuthorizationService.CI_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CI_DISABLED", "CI observation agent is disabled");
        }
        if (request == null || request.taskId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CI_INVALID_REQUEST", "Task id is required");
        }

        Instant startedAt = Instant.now();
        List<TimelineEvent> timeline = new ArrayList<>();
        timeline.add(new TimelineEvent("STARTED", startedAt, "CI observation agent started"));

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));

        PullRequestOperation pullRequestOperation =
                pullRequestStorageService.findLatest(task.getId(), user.getOrganizationId());
        if (pullRequestOperation == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CI_PR_OPERATION_NOT_FOUND",
                    "No pull request operation found for task");
        }
        if (pullRequestOperation.status() != PullRequestStatus.SUCCEEDED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CI_PR_OPERATION_NOT_SUCCEEDED",
                    "Pull request operation has not succeeded");
        }
        if (pullRequestOperation.pullRequestNumber() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CI_INVALID_REQUEST",
                    "Pull request number is required for CI observation");
        }

        ResolvedRepositoryConfig config = resolveRepositoryConfig(task.getOrganizationId(), task.getProjectId());
        String effectiveProvider = resolveEffectiveProvider(config);
        String token = resolveToken(config);

        UUID operationId = UUID.randomUUID();
        storageService.startPending(operationId, task, pullRequestOperation, config, startedAt, timeline);
        timeline.add(new TimelineEvent("PENDING", Instant.now(), "Operation " + operationId));

        try {
            storageService.updateStatus(operationId, CiObservationStatus.FETCHING, timeline);

            CiProvider provider = providerRegistry.requireProvider(effectiveProvider);
            RepositoryRef repositoryRef = config.repositoryRef();
            String commitHash = pullRequestOperation.remoteCommitHash() != null
                    ? pullRequestOperation.remoteCommitHash()
                    : pullRequestOperation.localCommitHash();

            WorkflowRunQuery query = new WorkflowRunQuery(
                    pullRequestOperation.sourceBranch(),
                    commitHash,
                    pullRequestOperation.pullRequestNumber(),
                    Math.max(1, properties.getMaxRunsPerObservation()));

            List<ProviderWorkflowRun> runs = provider.listWorkflowRuns(repositoryRef, query, token);
            List<List<ProviderJob>> jobsPerRun = new ArrayList<>();
            for (ProviderWorkflowRun run : runs) {
                jobsPerRun.add(provider.listJobs(repositoryRef, run.externalRunId(), token));
            }

            storageService.updateStatus(operationId, CiObservationStatus.PROCESSING, timeline);

            AggregatedHealth health = healthAggregator.aggregate(runs, jobsPerRun);

            CiObservationOperation result = storageService.markSucceeded(
                    operationId,
                    health.overallStatus(),
                    health.failureSummary(),
                    health.retryRecommendation(),
                    health.failureSummaryText(),
                    runs,
                    jobsPerRun,
                    Instant.now(),
                    timeline);
            timeline.add(new TimelineEvent("COMPLETED", Instant.now(), "SUCCEEDED"));
            return result;
        } catch (ApiException ex) {
            storageService.markFailed(operationId, ex.getCode(), ex.getMessage(), Instant.now(), timeline);
            throw ex;
        } catch (RuntimeException ex) {
            storageService.markFailed(operationId, "CI_FETCH_FAILED", ex.getMessage(), Instant.now(), timeline);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public CiObservationOperation getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, CiAuthorizationService.CI_READ);
        requireTask(taskId, user.getOrganizationId());
        CiObservationOperation result = storageService.findLatest(taskId, user.getOrganizationId());
        if (result == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "CI_NOT_FOUND", "No CI observation operation found for task");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CiObservationOperation> getHistory(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, CiAuthorizationService.CI_READ);
        requireTask(taskId, user.getOrganizationId());
        return storageService.findHistory(taskId, user.getOrganizationId());
    }

    private ResolvedRepositoryConfig resolveRepositoryConfig(UUID organizationId, UUID projectId) {
        try {
            return repositoryConfigService.resolve(organizationId, projectId);
        } catch (ApiException ex) {
            throw remapConfigError(ex);
        }
    }

    private static ApiException remapConfigError(ApiException ex) {
        String code = switch (ex.getCode()) {
            case "PR_REMOTE_NOT_CONFIGURED" -> "CI_REMOTE_NOT_CONFIGURED";
            case "PR_PROVIDER_UNSUPPORTED" -> "CI_PROVIDER_UNSUPPORTED";
            case "PR_CREDENTIALS_MISSING" -> "CI_CREDENTIALS_MISSING";
            case "PR_REPOSITORY_MISMATCH" -> "CI_REPOSITORY_MISMATCH";
            default -> ex.getCode().startsWith("PR_") ? "CI_REMOTE_NOT_CONFIGURED" : ex.getCode();
        };
        return new ApiException(ex.getStatus(), code, ex.getMessage());
    }

    private String resolveEffectiveProvider(ResolvedRepositoryConfig config) {
        String configuredProperty = properties.getProvider() == null
                ? "GITHUB"
                : properties.getProvider().trim().toUpperCase(Locale.ROOT);
        if ("LOCAL".equals(configuredProperty)) {
            return "LOCAL";
        }
        String fromConfig = config.effectiveProvider();
        if (!providerRegistry.supports(fromConfig)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CI_PROVIDER_UNSUPPORTED", "Provider is not supported: " + fromConfig);
        }
        return fromConfig;
    }

    private String resolveToken(ResolvedRepositoryConfig config) {
        if (config.fileRemote()) {
            return properties.getGithubToken() == null || properties.getGithubToken().isBlank()
                    ? "local-no-token"
                    : properties.getGithubToken().trim();
        }
        String token = properties.getGithubToken();
        if (token == null || token.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "CI_CREDENTIALS_MISSING", "CI provider credentials are not configured");
        }
        return token.trim();
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CI_INVALID_REQUEST", "Orchestration task not found"));
    }
}
