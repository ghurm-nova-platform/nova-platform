package ai.nova.platform.testing.service;

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
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.config.TestingProperties;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.dto.TestingDtos.TestingPromptContext;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.dto.TestingDtos.TestingRunRequest;
import ai.nova.platform.testing.security.TestingAuthorizationService;
import ai.nova.platform.web.error.ApiException;

/**
 * Testing Agent: generates structured test plans/cases for Coding artifacts.
 * Never executes tests, shell, build tools, Docker, or repository mutations.
 */
@Service
public class TestingAgentService {

    private final TestingAuthorizationService authorizationService;
    private final TestingProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final ArtifactStorageService artifactStorageService;
    private final ReviewStorageService reviewStorageService;
    private final AgentRuntimeClient agentRuntimeClient;
    private final TestingPromptBuilder promptBuilder;
    private final TestingJsonParser jsonParser;
    private final TestingValidator validator;
    private final TestingStorageService storageService;

    public TestingAgentService(
            TestingAuthorizationService authorizationService,
            TestingProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            AgentOrchestrationRunRepository runRepository,
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            ArtifactStorageService artifactStorageService,
            ReviewStorageService reviewStorageService,
            AgentRuntimeClient agentRuntimeClient,
            TestingPromptBuilder promptBuilder,
            TestingJsonParser jsonParser,
            TestingValidator validator,
            TestingStorageService storageService) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.artifactStorageService = artifactStorageService;
        this.reviewStorageService = reviewStorageService;
        this.agentRuntimeClient = agentRuntimeClient;
        this.promptBuilder = promptBuilder;
        this.jsonParser = jsonParser;
        this.validator = validator;
        this.storageService = storageService;
    }

    public TestingResult run(TestingRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, TestingAuthorizationService.TESTING_RUN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TESTING_DISABLED", "Testing agent is disabled");
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
                    "TESTING_NO_ARTIFACTS",
                    "No generated artifacts found for task; run Coding Agent first");
        }

        ReviewResult review = reviewStorageService.findLatest(task.getId(), user.getOrganizationId());
        List<ReviewFinding> findings = review == null ? List.of() : review.findings();

        TestingPromptContext promptContext = new TestingPromptContext(
                task.getId(),
                task.getTaskKey(),
                task.getDisplayName(),
                task.getDescription(),
                run.getObjective() == null ? "" : run.getObjective(),
                artifacts,
                findings,
                Map.of("organizationId", project.getOrganizationId().toString()),
                projectSettings(project));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(promptContext);

        Agent testingAgent = null;
        if (task.getAssignedAgentId() != null) {
            testingAgent = agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            task.getAssignedAgentId(), project.getId(), project.getOrganizationId())
                    .orElse(null);
        }
        String provider = testingAgent != null ? testingAgent.getModelProvider() : properties.getDefaultProvider();
        String model = resolveModel(testingAgent, task);
        if (testingAgent != null
                && testingAgent.getSystemPrompt() != null
                && !testingAgent.getSystemPrompt().isBlank()) {
            systemPrompt = testingAgent.getSystemPrompt().trim() + "\n\n" + systemPrompt;
        }

        ExecutionRequest executionRequest = new ExecutionRequest(
                project.getOrganizationId(),
                project.getId(),
                testingAgent != null ? testingAgent.getId() : null,
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
                    "TESTING_INVALID_JSON",
                    "Testing agent must return a final JSON response without tool calls");
        }
        RuntimeFinalResponse finalResponse = turn.finalResponse();
        ParsedTestingOutput parsed = jsonParser.parse(finalResponse.responseText());
        validator.validate(parsed);

        return storageService.replaceResult(
                task,
                artifacts,
                parsed,
                (long) finalResponse.totalTokens(),
                model,
                provider,
                generationTimeMs);
    }

    @Transactional(readOnly = true)
    public TestingResult getLatest(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, TestingAuthorizationService.TESTING_READ);
        requireTask(taskId, user.getOrganizationId());
        TestingResult result = storageService.findLatest(taskId, user.getOrganizationId());
        if (result == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TESTING_NOT_FOUND", "No testing result found for task");
        }
        return result;
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "TESTING_TASK_NOT_FOUND", "Orchestration task not found"));
    }

    private String resolveModel(Agent testingAgent, AgentOrchestrationTask task) {
        if (testingAgent != null && testingAgent.getModelName() != null && !testingAgent.getModelName().isBlank()) {
            return testingAgent.getModelName();
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
