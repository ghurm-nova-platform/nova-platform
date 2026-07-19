package ai.nova.platform.pullrequest.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.GitStorageService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRunRequest;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.TimelineEvent;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.entity.RemotePushStatus;
import ai.nova.platform.pullrequest.provider.CreatePullRequestRequest;
import ai.nova.platform.pullrequest.provider.InMemoryPullRequestProvider;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.provider.PullRequestProvider;
import ai.nova.platform.pullrequest.provider.PullRequestProviderRegistry;
import ai.nova.platform.pullrequest.provider.RemoteBranchInfo;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.pullrequest.security.PullRequestAuthorizationService;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService.ResolvedRepositoryConfig;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.service.TestingStorageService;
import ai.nova.platform.web.error.ApiException;

/**
 * Pull Request Agent: publishes successful Git Integration branches and creates reviewable pull requests.
 * Never merges, approves, force-pushes, or mutates protected branches.
 */
@Service
public class PullRequestAgentService {

    private final PullRequestAuthorizationService authorizationService;
    private final PullRequestProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final GitStorageService gitStorageService;
    private final PatchStorageService patchStorageService;
    private final ReviewStorageService reviewStorageService;
    private final TestingStorageService testingStorageService;
    private final PullRequestGitValidator gitValidator;
    private final ProjectRepositoryConfigService repositoryConfigService;
    private final PullRequestProviderRegistry providerRegistry;
    private final PullRequestRemoteGitService remoteGitService;
    private final PullRequestBodyBuilder bodyBuilder;
    private final PullRequestStorageService storageService;

    public PullRequestAgentService(
            PullRequestAuthorizationService authorizationService,
            PullRequestProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            ProjectRepository projectRepository,
            GitStorageService gitStorageService,
            PatchStorageService patchStorageService,
            ReviewStorageService reviewStorageService,
            TestingStorageService testingStorageService,
            PullRequestGitValidator gitValidator,
            ProjectRepositoryConfigService repositoryConfigService,
            PullRequestProviderRegistry providerRegistry,
            PullRequestRemoteGitService remoteGitService,
            PullRequestBodyBuilder bodyBuilder,
            PullRequestStorageService storageService) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.gitStorageService = gitStorageService;
        this.patchStorageService = patchStorageService;
        this.reviewStorageService = reviewStorageService;
        this.testingStorageService = testingStorageService;
        this.gitValidator = gitValidator;
        this.repositoryConfigService = repositoryConfigService;
        this.providerRegistry = providerRegistry;
        this.remoteGitService = remoteGitService;
        this.bodyBuilder = bodyBuilder;
        this.storageService = storageService;
    }

    public PullRequestOperation run(PullRequestRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PullRequestAuthorizationService.PR_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PR_DISABLED", "Pull request agent is disabled");
        }
        if (request == null || request.taskId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PR_INVALID_REQUEST", "Task id is required");
        }

        Instant startedAt = Instant.now();
        List<TimelineEvent> timeline = new ArrayList<>();
        timeline.add(new TimelineEvent("STARTED", startedAt, "Pull request agent started"));

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));

        GitOperation gitOperation = gitStorageService.findLatest(task.getId(), user.getOrganizationId());
        if (gitOperation == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_GIT_OPERATION_NOT_FOUND", "No git operation found for task");
        }

        PatchResult patch = patchStorageService.findLatest(task.getId(), user.getOrganizationId());
        ReviewResult review = reviewStorageService.findLatest(task.getId(), user.getOrganizationId());
        TestingResult testing = testingStorageService.findLatest(task.getId(), user.getOrganizationId());

        gitValidator.validateWorkspace(gitOperation, task, patch);

        ResolvedRepositoryConfig config = repositoryConfigService.resolve(task.getOrganizationId(), task.getProjectId());

        PullRequestOperation existingSucceeded =
                storageService.findSucceededByGitOperationId(gitOperation.id());
        if (existingSucceeded != null) {
            return existingSucceeded;
        }

        UUID operationId = UUID.randomUUID();
        storageService.startPending(
                operationId,
                task,
                gitOperation.id(),
                gitOperation.patchResultId(),
                config,
                gitOperation.branchName(),
                gitOperation.commitHash(),
                gitOperation.patchHash(),
                startedAt,
                timeline);
        timeline.add(new TimelineEvent("PENDING", Instant.now(), "Operation " + operationId));

        boolean pushRecorded = false;
        String remoteCommitHash = null;
        try {
            storageService.updateStatus(operationId, PullRequestStatus.VALIDATING, timeline);

            PullRequestProvider provider = providerRegistry.requireProvider(config.effectiveProvider());
            RepositoryRef repositoryRef = config.repositoryRef();
            if (!config.fileRemote()) {
                provider.validateRepository(repositoryRef, config.tokenOrNull());
            }

            Optional<RemoteBranchInfo> remoteBranch = resolveRemoteBranch(config, provider, repositoryRef, gitOperation);
            if (remoteBranch.isPresent()) {
                if (!matchesCommit(remoteBranch.get().commitHash(), gitOperation.commitHash())) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "PR_REMOTE_BRANCH_CONFLICT",
                            "Remote branch points to a different commit");
                }
                remoteCommitHash = remoteBranch.get().commitHash();
            }

            Optional<ProviderPullRequest> existingPullRequest = provider.findExistingPullRequest(
                    repositoryRef, gitOperation.branchName(), config.targetBranch(), config.tokenOrNull());
            if (existingPullRequest.isPresent()
                    && matchesCommit(existingPullRequest.get().headSha(), gitOperation.commitHash())) {
                remoteCommitHash = normalizeCommit(existingPullRequest.get().headSha());
                pushRecorded = recordSkippedPush(
                        operationId, config, gitOperation, remoteCommitHash, timeline);
                return finalizeSucceeded(
                        operationId,
                        provider,
                        repositoryRef,
                        config,
                        gitOperation,
                        existingPullRequest.get(),
                        remoteCommitHash,
                        timeline);
            }

            if (remoteBranch.isEmpty()) {
                storageService.updateStatus(operationId, PullRequestStatus.PUSHING, timeline);
                Instant pushStartedAt = Instant.now();
                Path localRepo = Path.of(gitOperation.repositoryPath());
                remoteCommitHash = remoteGitService.pushExactBranch(
                        localRepo,
                        config.remoteUrl(),
                        gitOperation.branchName(),
                        gitOperation.commitHash(),
                        config.tokenOrNull());
                storageService.markPushed(
                        operationId,
                        remoteCommitHash,
                        config.remoteName(),
                        gitOperation.branchName(),
                        gitOperation.commitHash(),
                        RemotePushStatus.SUCCEEDED,
                        pushStartedAt,
                        Instant.now(),
                        timeline);
                pushRecorded = true;
            } else {
                pushRecorded = recordSkippedPush(
                        operationId, config, gitOperation, remoteCommitHash, timeline);
            }

            syncInMemoryBranch(provider, repositoryRef, gitOperation.branchName(), remoteCommitHash);

            storageService.updateStatus(operationId, PullRequestStatus.CREATING_PR, timeline);

            String title = bodyBuilder.buildTitle(task);
            String body = bodyBuilder.buildBody(task, gitOperation, patch, review, testing, config.targetBranch());
            ProviderPullRequest created = provider.createPullRequest(
                    new CreatePullRequestRequest(
                            repositoryRef,
                            title,
                            body,
                            gitOperation.branchName(),
                            config.targetBranch(),
                            properties.isDraftByDefault()),
                    config.tokenOrNull());

            return finalizeSucceeded(
                    operationId, provider, repositoryRef, config, gitOperation, created, remoteCommitHash, timeline);
        } catch (ApiException ex) {
            if (pushRecorded) {
                storageService.updateStatus(operationId, PullRequestStatus.PUSHED, timeline);
            }
            storageService.markFailed(operationId, ex.getCode(), ex.getMessage(), Instant.now(), timeline);
            throw ex;
        } catch (RuntimeException ex) {
            if (pushRecorded) {
                storageService.updateStatus(operationId, PullRequestStatus.PUSHED, timeline);
            }
            storageService.markFailed(
                    operationId, "PR_CREATE_FAILED", ex.getMessage(), Instant.now(), timeline);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PullRequestOperation getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, PullRequestAuthorizationService.PR_READ);
        requireTask(taskId, user.getOrganizationId());
        PullRequestOperation result = storageService.findLatest(taskId, user.getOrganizationId());
        if (result == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PR_NOT_FOUND", "No pull request operation found for task");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<PullRequestOperation> getHistory(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, PullRequestAuthorizationService.PR_READ);
        requireTask(taskId, user.getOrganizationId());
        return storageService.findHistory(taskId, user.getOrganizationId());
    }

    private PullRequestOperation finalizeSucceeded(
            UUID operationId,
            PullRequestProvider provider,
            RepositoryRef repositoryRef,
            ResolvedRepositoryConfig config,
            GitOperation gitOperation,
            ProviderPullRequest candidate,
            String remoteCommitHash,
            List<TimelineEvent> timeline) {
        ProviderPullRequest verified =
                provider.getPullRequest(repositoryRef, candidate.number(), config.tokenOrNull());
        verifyPullRequest(verified, gitOperation, config.targetBranch());
        PullRequestOperation result = storageService.markSucceeded(
                operationId, verified, remoteCommitHash, Instant.now(), timeline);
        timeline.add(new TimelineEvent("COMPLETED", Instant.now(), "SUCCEEDED"));
        return result;
    }

    private boolean recordSkippedPush(
            UUID operationId,
            ResolvedRepositoryConfig config,
            GitOperation gitOperation,
            String remoteCommitHash,
            List<TimelineEvent> timeline) {
        Instant now = Instant.now();
        storageService.markPushed(
                operationId,
                remoteCommitHash,
                config.remoteName(),
                gitOperation.branchName(),
                gitOperation.commitHash(),
                RemotePushStatus.SKIPPED,
                now,
                now,
                timeline);
        return true;
    }

    private static void verifyPullRequest(
            ProviderPullRequest pullRequest, GitOperation gitOperation, String targetBranch) {
        if (!pullRequest.sourceBranch().equals(gitOperation.branchName())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "PR_VERIFY_FAILED", "Pull request source branch mismatch");
        }
        if (!pullRequest.targetBranch().equals(targetBranch)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "PR_VERIFY_FAILED", "Pull request target branch mismatch");
        }
        if (!matchesCommit(pullRequest.headSha(), gitOperation.commitHash())) {
            throw new ApiException(HttpStatus.CONFLICT, "PR_VERIFY_FAILED", "Pull request head commit mismatch");
        }
    }

    private static void syncInMemoryBranch(
            PullRequestProvider provider, RepositoryRef repositoryRef, String branchName, String commitHash) {
        if (provider instanceof InMemoryPullRequestProvider inMemory) {
            inMemory.recordRemoteBranch(repositoryRef, branchName, commitHash);
        }
    }

    private static boolean matchesCommit(String left, String right) {
        return normalizeCommit(left).equals(normalizeCommit(right));
    }

    private static String normalizeCommit(String hash) {
        return hash == null ? "" : hash.trim().toLowerCase();
    }

    private Optional<RemoteBranchInfo> resolveRemoteBranch(
            ResolvedRepositoryConfig config,
            PullRequestProvider provider,
            RepositoryRef repositoryRef,
            GitOperation gitOperation) {
        if (config.fileRemote()) {
            return remoteGitService
                    .findRemoteHeadCommit(
                            config.remoteUrl(), gitOperation.branchName(), config.tokenOrNull())
                    .map(commit -> new RemoteBranchInfo(gitOperation.branchName(), commit));
        }
        return provider.findRemoteBranch(
                repositoryRef, gitOperation.branchName(), config.tokenOrNull());
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PR_INVALID_REQUEST", "Orchestration task not found"));
    }
}
