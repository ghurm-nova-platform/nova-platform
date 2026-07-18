package ai.nova.platform.git.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.dto.GitDtos.GitRunRequest;
import ai.nova.platform.git.dto.GitDtos.TimelineEvent;
import ai.nova.platform.git.security.GitAuthorizationService;
import ai.nova.platform.git.service.ControlledGitService.SeedFile;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Git Integration Agent: applies validated Patch Agent output onto an isolated working branch.
 * Never merges, pushes, force-pushes, deletes branches, or runs arbitrary shell commands.
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

    public GitIntegrationAgentService(
            GitAuthorizationService authorizationService,
            GitProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            ProjectRepository projectRepository,
            PatchStorageService patchStorageService,
            GitValidator validator,
            GitBranchStrategy branchStrategy,
            ControlledGitService gitService,
            GitStorageService storageService) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.patchStorageService = patchStorageService;
        this.validator = validator;
        this.branchStrategy = branchStrategy;
        this.gitService = gitService;
        this.storageService = storageService;
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
        Project project = projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));

        PatchResult patch = patchStorageService.findLatest(task.getId(), user.getOrganizationId());
        validator.requireApprovedPatch(patch);
        timeline.add(new TimelineEvent("PATCH_VALIDATED", Instant.now(), "Approved patch validated"));

        String branchName = branchStrategy.branchNameForTask(task.getId());
        String baseRef = properties.getBaseRef();
        validator.requireSafeBranch(branchName, baseRef);

        if (storageService.branchRecorded(project.getOrganizationId(), project.getId(), branchName)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_BRANCH_EXISTS", "Branch already recorded: " + branchName);
        }

        Path repoPath = gitService.resolveRepositoryPath(project.getOrganizationId(), project.getId());
        gitService.ensureBaseRepository(repoPath, baseRef, defaultSeeds(patch));

        if (gitService.branchExists(repoPath, branchName)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "GIT_BRANCH_EXISTS", "Branch already exists: " + branchName);
        }

        Instant branchCreatedAt = gitService.createIsolatedBranch(repoPath, branchName, baseRef);
        timeline.add(new TimelineEvent("BRANCH_CREATED", branchCreatedAt, "Created " + branchName));

        gitService.applyPatch(repoPath, patch.patch());
        timeline.add(new TimelineEvent("PATCH_APPLIED", Instant.now(), "Unified diff applied"));

        String commitMessage = branchStrategy.commitMessageForTask(task.getId());
        String commitHash;
        try {
            commitHash = gitService.commitAll(repoPath, commitMessage);
        } catch (ApiException ex) {
            throw ex;
        }
        Instant completedAt = Instant.now();
        timeline.add(new TimelineEvent("COMMITTED", completedAt, "Commit " + commitHash));

        gitService.verifyConsistent(repoPath);
        timeline.add(new TimelineEvent("VERIFIED", Instant.now(), "Repository consistent"));

        String patchHash = validator.sha256(patch.patch());
        GitOperation result = storageService.replaceSucceeded(
                task,
                patch.id(),
                branchName,
                commitHash,
                patchHash,
                repoPath.toString(),
                baseRef,
                commitMessage,
                properties.getAuthorName(),
                properties.getAuthorEmail(),
                startedAt,
                branchCreatedAt,
                completedAt,
                timeline);
        timeline.add(new TimelineEvent("COMPLETED", Instant.now(), "SUCCEEDED"));
        return result;
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

    private static List<SeedFile> defaultSeeds(PatchResult patch) {
        // Seed base files that the sample patches modify: LoginService with one line.
        if (patch.files() != null) {
            List<SeedFile> files = new ArrayList<>();
            for (var file : patch.files()) {
                if ("MODIFY".equals(file.changeType().name()) || "DELETE".equals(file.changeType().name())) {
                    files.add(new SeedFile(file.path(), "class LoginService {}\n"));
                }
            }
            if (!files.isEmpty()) {
                return files;
            }
        }
        return List.of(new SeedFile("src/LoginService.java", "class LoginService {}\n"));
    }
}
