package ai.nova.platform.git.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
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
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.dto.GitDtos.GitRunRequest;
import ai.nova.platform.git.dto.GitDtos.TimelineEvent;
import ai.nova.platform.git.security.GitAuthorizationService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Git Integration Agent: applies validated Patch Agent output onto an isolated operation workspace.
 * Never mutates the shared project source tree. Never merges, pushes, force-pushes, or deletes branches.
 */
@Service
public class GitIntegrationAgentService {

    private final GitAuthorizationService authorizationService;
    private final GitProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final PatchStorageService patchStorageService;
    private final GitValidator validator;
    private final GitBranchStrategy branchStrategy;
    private final ControlledGitService gitService;
    private final GitStorageService storageService;
    private final AuditRecordingSupport auditRecordingSupport;

    public GitIntegrationAgentService(
            GitAuthorizationService authorizationService,
            GitProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            ProjectRepository projectRepository,
            PatchStorageService patchStorageService,
            GitValidator validator,
            GitBranchStrategy branchStrategy,
            ControlledGitService gitService,
            GitStorageService storageService,
            AuditRecordingSupport auditRecordingSupport) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.patchStorageService = patchStorageService;
        this.validator = validator;
        this.branchStrategy = branchStrategy;
        this.gitService = gitService;
        this.storageService = storageService;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    public GitOperation run(GitRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, GitAuthorizationService.GIT_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_DISABLED", "Git integration agent is disabled");
        }

        Instant startedAt = Instant.now();
        List<TimelineEvent> timeline = new ArrayList<>();
        timeline.add(new TimelineEvent("STARTED", startedAt, "Git integration started"));

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        publishTaskAudit(user, task, AuditAction.CREATE, AuditResult.SUCCESS, Map.of());
        Project project = projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));

        PatchResult patch = patchStorageService.findLatest(task.getId(), user.getOrganizationId());
        validator.requireApprovedPatch(patch);
        String patchHash = validator.sha256(patch.patch());
        timeline.add(new TimelineEvent("PATCH_VALIDATED", Instant.now(), "Approved patch validated"));

        String branchName = branchStrategy.branchNameForTask(task.getId());
        String baseRef = properties.getBaseRef();
        validator.requireSafeBranch(branchName, baseRef);

        if (storageService.branchRecorded(project.getOrganizationId(), project.getId(), branchName)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_BRANCH_EXISTS", "Branch already recorded: " + branchName);
        }

        Path sourcePath =
                gitService.resolveSourceRepositoryPath(project.getOrganizationId(), project.getId());
        // Validate source + baseRef before allocating an operation (no synthetic init).
        gitService.requireSourceRepository(sourcePath, baseRef);

        UUID operationId = UUID.randomUUID();
        Path operationRepoPath = gitService.resolveOperationRepositoryPath(
                project.getOrganizationId(), project.getId(), operationId);

        GitOperation pending = storageService.startPending(
                operationId,
                task,
                patch.id(),
                branchName,
                patchHash,
                operationRepoPath.toString(),
                baseRef,
                startedAt,
                timeline);
        timeline.add(new TimelineEvent(
                "WORKSPACE_ALLOCATED", Instant.now(), "Operation workspace " + operationId));

        Instant branchCreatedAt = null;
        try {
            gitService.cloneSourceToOperationWorkspace(sourcePath, operationRepoPath);
            timeline.add(new TimelineEvent("WORKSPACE_CLONED", Instant.now(), "Cloned source into isolation"));

            if (gitService.branchExists(operationRepoPath, branchName)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "GIT_BRANCH_EXISTS", "Branch already exists: " + branchName);
            }

            branchCreatedAt = gitService.createIsolatedBranch(operationRepoPath, branchName, baseRef);
            timeline.add(new TimelineEvent("BRANCH_CREATED", branchCreatedAt, "Created " + branchName));

            gitService.applyPatch(operationRepoPath, patch.patch());
            timeline.add(new TimelineEvent("PATCH_APPLIED", Instant.now(), "Unified diff applied"));

            String commitMessage = branchStrategy.commitMessageForTask(task.getId());
            String commitHash = gitService.commitAll(operationRepoPath, commitMessage);
            Instant completedAt = Instant.now();
            timeline.add(new TimelineEvent("COMMITTED", completedAt, "Commit " + commitHash));

            gitService.verifySuccessfulCommit(operationRepoPath, branchName, commitHash, baseRef);
            timeline.add(new TimelineEvent("VERIFIED", Instant.now(), "Repository consistent"));

            GitOperation result = storageService.markSucceeded(
                    pending.id(),
                    commitHash,
                    commitMessage,
                    properties.getAuthorName(),
                    properties.getAuthorEmail(),
                    branchCreatedAt,
                    completedAt,
                    timeline);
            timeline.add(new TimelineEvent("COMPLETED", Instant.now(), "SUCCEEDED"));
            publishTaskAudit(
                    user, task, AuditAction.COMPLETE, AuditResult.SUCCESS, Map.of("operationId", pending.id().toString()));
            return result;
        } catch (ApiException ex) {
            storageService.markFailed(pending.id(), ex.getCode(), ex.getMessage(), Instant.now());
            publishTaskAudit(user, task, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", ex.getCode()));
            throw ex;
        } catch (RuntimeException ex) {
            storageService.markFailed(
                    pending.id(), "GIT_REPO_INCONSISTENT", ex.getMessage(), Instant.now());
            publishTaskAudit(
                    user, task, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", "GIT_REPO_INCONSISTENT"));
            throw ex;
        }
        // Failed operation workspaces are preserved by default for diagnosis
        // (nova.git.cleanup-failed-workspaces=false). Source repository is never mutated.
    }

    @Transactional(readOnly = true)
    public GitOperation getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, GitAuthorizationService.GIT_READ);
        requireTask(taskId, user.getOrganizationId());
        GitOperation result = storageService.findLatest(taskId, user.getOrganizationId());
        if (result == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "GIT_NOT_FOUND", "No git operation found for task");
        }
        return result;
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "GIT_TASK_NOT_FOUND", "Orchestration task not found"));
    }

    private void publishTaskAudit(
            AuthenticatedUser user,
            AgentOrchestrationTask task,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        try {
            auditRecordingSupport.recordDomainEvent(
                    user,
                    task.getProjectId(),
                    AuditEntityType.TASK,
                    task.getId(),
                    task.getDisplayName(),
                    action,
                    result,
                    AuditSource.GIT_INTEGRATION,
                    details);
        } catch (RuntimeException ignored) {
            // AuditPublisher swallows failures; guard against unexpected propagation.
        }
    }
}
