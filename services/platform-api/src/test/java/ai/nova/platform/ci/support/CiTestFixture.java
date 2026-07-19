package ai.nova.platform.ci.support;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.entity.RemotePushStatus;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService.ResolvedRepositoryConfig;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

/**
 * Shared CI Observation Agent test fixture: seeds succeeded PR operations and in-memory CI data.
 */
public final class CiTestFixture {

    private CiTestFixture() {
    }

    public static AuthenticatedUser adminUser() {
        return new AuthenticatedUser(
                PullRequestTestFixture.USER_ID,
                PullRequestTestFixture.ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("GIT_RUN", "GIT_READ", "PR_RUN", "PR_READ", "CI_RUN", "CI_READ"),
                true);
    }

    public static PullRequestOperation seedSucceededPullRequestOperation(
            PullRequestStorageService pullRequestStorageService,
            ProjectRepositoryConfigService repositoryConfigService,
            AgentOrchestrationTask task,
            GitOperation gitOperation,
            long pullRequestNumber) {
        ResolvedRepositoryConfig config =
                repositoryConfigService.resolve(task.getOrganizationId(), task.getProjectId());
        Instant now = Instant.now();
        UUID operationId = UUID.randomUUID();
        String branch = gitOperation.branchName();

        pullRequestStorageService.startPending(
                operationId,
                task,
                gitOperation.id(),
                gitOperation.patchResultId(),
                config,
                branch,
                gitOperation.commitHash(),
                gitOperation.patchHash(),
                now,
                List.of());

        pullRequestStorageService.markPushed(
                operationId,
                gitOperation.commitHash(),
                "origin",
                branch,
                gitOperation.commitHash(),
                RemotePushStatus.SKIPPED,
                now,
                now,
                List.of());

        return pullRequestStorageService.markSucceeded(
                operationId,
                new ProviderPullRequest(
                        String.valueOf(pullRequestNumber),
                        pullRequestNumber,
                        "memory://ghurm-nova-platform/nova-demo/pull/" + pullRequestNumber,
                        "Test PR for CI",
                        branch,
                        config.targetBranch(),
                        "open",
                        gitOperation.commitHash()),
                gitOperation.commitHash(),
                now,
                List.of());
    }
}
