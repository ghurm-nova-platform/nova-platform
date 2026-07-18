package ai.nova.platform.review.service;

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
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.review.config.ReviewProperties;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.dto.ReviewDtos.ReviewPromptContext;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.dto.ReviewDtos.ReviewRunRequest;
import ai.nova.platform.review.security.ReviewAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Review Agent: evaluates generated artifacts and returns structured findings.
 * Never modifies artifacts, generates patches, executes shell, or edits git.
 */
@Service
public class ReviewAgentService {

    private final ReviewAuthorizationService authorizationService;
    private final ReviewProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final ArtifactStorageService artifactStorageService;
    private final AgentRuntimeClient agentRuntimeClient;
    private final ReviewPromptBuilder promptBuilder;
    private final ReviewJsonParser jsonParser;
    private final ReviewValidator validator;
    private final ReviewStorageService storageService;

    public ReviewAgentService(
            ReviewAuthorizationService authorizationService,
            ReviewProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            AgentOrchestrationRunRepository runRepository,
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            ArtifactStorageService artifactStorageService,
            AgentRuntimeClient agentRuntimeClient,
            ReviewPromptBuilder promptBuilder,
            ReviewJsonParser jsonParser,
            ReviewValidator validator,
            ReviewStorageService storageService) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.artifactStorageService = artifactStorageService;
        this.agentRuntimeClient = agentRuntimeClient;
        this.promptBuilder = promptBuilder;
        this.jsonParser = jsonParser;
        this.validator = validator;
        this.storageService = storageService;
    }

    public ReviewResult run(ReviewRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ReviewAuthorizationService.REVIEW_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "REVIEW_DISABLED", "Review agent is disabled");
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
                    "REVIEW_NO_ARTIFACTS",
                    "No generated artifacts found for task; run Coding Agent first");
        }

        ReviewPromptContext promptContext = new ReviewPromptContext(
                task.getId(),
                task.getTaskKey(),
                task.getDisplayName(),
                task.getDescription(),
                run.getObjective() == null ? "" : run.getObjective(),
                artifacts,
                Map.of("organizationId", project.getOrganizationId().toString()),
                projectSettings(project));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(promptContext);

        Agent reviewAgent = null;
        if (task.getAssignedAgentId() != null) {
            reviewAgent = agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            task.getAssignedAgentId(), project.getId(), project.getOrganizationId())
                    .orElse(null);
        }
        String provider = reviewAgent != null ? reviewAgent.getModelProvider() : properties.getDefaultProvider();
        String model = resolveModel(reviewAgent, task);
        if (reviewAgent != null
                && reviewAgent.getSystemPrompt() != null
                && !reviewAgent.getSystemPrompt().isBlank()) {
            systemPrompt = reviewAgent.getSystemPrompt().trim() + "\n\n" + systemPrompt;
        }

        ExecutionRequest executionRequest = new ExecutionRequest(
                project.getOrganizationId(),
                project.getId(),
                reviewAgent != null ? reviewAgent.getId() : null,
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
        // External AI call — keep outside DB write transactions.
        RuntimeTurnResult turn = agentRuntimeClient.execute(executionRequest);
        long reviewTimeMs = System.currentTimeMillis() - started;
        if (!turn.isFinal() || turn.finalResponse() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REVIEW_INVALID_JSON",
                    "Review agent must return a final JSON response without tool calls");
        }
        RuntimeFinalResponse finalResponse = turn.finalResponse();
        ParsedReviewOutput parsed = jsonParser.parse(finalResponse.responseText());
        validator.validate(parsed);

        return storageService.replaceReview(
                task,
                artifacts,
                parsed,
                (long) finalResponse.totalTokens(),
                model,
                provider,
                reviewTimeMs);
    }

    @Transactional(readOnly = true)
    public ReviewResult getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, ReviewAuthorizationService.REVIEW_READ);
        requireTask(taskId, user.getOrganizationId());
        ReviewResult result = storageService.findLatest(taskId, user.getOrganizationId());
        if (result == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "No review found for task");
        }
        return result;
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "REVIEW_TASK_NOT_FOUND", "Orchestration task not found"));
    }

    private String resolveModel(Agent reviewAgent, AgentOrchestrationTask task) {
        if (reviewAgent != null && reviewAgent.getModelName() != null && !reviewAgent.getModelName().isBlank()) {
            return reviewAgent.getModelName();
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
