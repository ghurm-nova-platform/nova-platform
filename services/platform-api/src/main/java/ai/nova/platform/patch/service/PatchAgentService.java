package ai.nova.platform.patch.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.config.PatchProperties;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.dto.PatchDtos.PatchPromptContext;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.dto.PatchDtos.PatchRunRequest;
import ai.nova.platform.patch.security.PatchAuthorizationService;
import ai.nova.platform.patch.service.PatchDiffParser.ParsedDiff;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTest;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.service.TestingStorageService;
import ai.nova.platform.web.error.ApiException;

/**
 * Patch Agent: generates Git-compatible Unified Diff patches from approved artifacts.
 * Never applies patches, executes git/shell, commits, pushes, or modifies repositories.
 */
@Service
public class PatchAgentService {

    private final PatchAuthorizationService authorizationService;
    private final PatchProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final ArtifactStorageService artifactStorageService;
    private final ReviewStorageService reviewStorageService;
    private final TestingStorageService testingStorageService;
    private final AgentRuntimeClient agentRuntimeClient;
    private final PatchPromptBuilder promptBuilder;
    private final PatchJsonParser jsonParser;
    private final PatchValidator validator;
    private final PatchStorageService storageService;

    public PatchAgentService(
            PatchAuthorizationService authorizationService,
            PatchProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            AgentOrchestrationRunRepository runRepository,
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            ArtifactStorageService artifactStorageService,
            ReviewStorageService reviewStorageService,
            TestingStorageService testingStorageService,
            AgentRuntimeClient agentRuntimeClient,
            PatchPromptBuilder promptBuilder,
            PatchJsonParser jsonParser,
            PatchValidator validator,
            PatchStorageService storageService) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.artifactStorageService = artifactStorageService;
        this.reviewStorageService = reviewStorageService;
        this.testingStorageService = testingStorageService;
        this.agentRuntimeClient = agentRuntimeClient;
        this.promptBuilder = promptBuilder;
        this.jsonParser = jsonParser;
        this.validator = validator;
        this.storageService = storageService;
    }

    public PatchResult run(PatchRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PatchAuthorizationService.PATCH_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PATCH_DISABLED", "Patch agent is disabled");
        }

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        AgentOrchestrationRun run = runRepository
                .findByIdAndOrganizationId(task.getRunId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORCHESTRATION_RUN_NOT_FOUND", "Run not found"));
        Project project = projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));

        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(task.getId(), user.getOrganizationId());
        if (artifacts.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH_NO_ARTIFACTS",
                    "No generated artifacts found for task; run Coding Agent first");
        }

        ReviewResult review = reviewStorageService.findLatest(task.getId(), user.getOrganizationId());
        if (review == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH_NO_REVIEW",
                    "No review result found for task; run Review Agent first");
        }
        if (!review.approved()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH_NOT_APPROVED",
                    "Latest review is not approved; Patch Agent requires approved artifacts");
        }

        TestingResult testing = testingStorageService.findLatest(task.getId(), user.getOrganizationId());
        List<ReviewFinding> findings = review.findings() == null ? List.of() : review.findings();
        List<GeneratedTest> tests =
                testing == null || testing.generatedTests() == null ? List.of() : testing.generatedTests();

        PatchPromptContext promptContext = new PatchPromptContext(
                task.getId(),
                task.getTaskKey(),
                task.getDisplayName(),
                task.getDescription(),
                run.getObjective() == null ? "" : run.getObjective(),
                artifacts,
                review.approved(),
                review.score(),
                review.summary(),
                findings,
                testing == null ? null : testing.coverageEstimate(),
                testing == null ? null : testing.summary(),
                tests,
                Map.of("organizationId", project.getOrganizationId().toString()),
                projectSettings(project));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(promptContext);

        Agent patchAgent = null;
        if (task.getAssignedAgentId() != null) {
            patchAgent = agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            task.getAssignedAgentId(), project.getId(), project.getOrganizationId())
                    .orElse(null);
        }
        String provider = patchAgent != null ? patchAgent.getModelProvider() : properties.getDefaultProvider();
        String model = resolveModel(patchAgent, task);
        if (patchAgent != null
                && patchAgent.getSystemPrompt() != null
                && !patchAgent.getSystemPrompt().isBlank()) {
            systemPrompt = patchAgent.getSystemPrompt().trim() + "\n\n" + systemPrompt;
        }

        ExecutionRequest executionRequest = new ExecutionRequest(
                project.getOrganizationId(),
                project.getId(),
                patchAgent != null ? patchAgent.getId() : null,
                UUID.randomUUID(),
                provider,
                model,
                systemPrompt,
                List.of(new RuntimeMessage("USER", userPrompt)),
                null,
                List.of(),
                List.of(),
                null);

        long started = System.currentTimeMillis();
        RuntimeTurnResult turn = agentRuntimeClient.execute(executionRequest);
        long generationTimeMs = System.currentTimeMillis() - started;
        if (!turn.isFinal() || turn.finalResponse() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH_INVALID_JSON",
                    "Patch agent must return a final JSON response without tool calls");
        }
        RuntimeFinalResponse finalResponse = turn.finalResponse();
        ParsedPatchOutput parsed = jsonParser.parse(finalResponse.responseText());
        ParsedDiff diff = validator.validate(parsed);

        return storageService.replaceResult(
                task,
                artifacts,
                parsed,
                diff,
                (long) finalResponse.totalTokens(),
                model,
                provider,
                generationTimeMs);
    }

    @Transactional(readOnly = true)
    public PatchResult getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, PatchAuthorizationService.PATCH_READ);
        requireTask(taskId, user.getOrganizationId());
        PatchResult result = storageService.findLatest(taskId, user.getOrganizationId());
        if (result == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PATCH_NOT_FOUND", "No patch result found for task");
        }
        return result;
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PATCH_TASK_NOT_FOUND", "Orchestration task not found"));
    }

    private String resolveModel(Agent patchAgent, AgentOrchestrationTask task) {
        if (patchAgent != null && patchAgent.getModelName() != null && !patchAgent.getModelName().isBlank()) {
            return patchAgent.getModelName();
        }
        if (task.getModelReference() != null && !task.getModelReference().isBlank()) {
            return task.getModelReference().trim();
        }
        return properties.getDefaultModel();
    }

    private static Map<String, String> projectSettings(Project project) {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("projectId", project.getId().toString());
        settings.put("projectName", project.getName());
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            String description = project.getDescription();
            settings.put(
                    "projectDescription",
                    description.length() <= 500 ? description : description.substring(0, 500));
        }
        settings.put("visibility", project.getVisibility().name());
        return settings;
    }
}
