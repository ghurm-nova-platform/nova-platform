package ai.nova.platform.coding.service;

import java.util.ArrayList;
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
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.coding.config.CodingProperties;
import ai.nova.platform.coding.dto.CodingDtos.CodeGenerationRequest;
import ai.nova.platform.coding.dto.CodingDtos.CodingPromptContext;
import ai.nova.platform.coding.dto.CodingDtos.CodingResult;
import ai.nova.platform.coding.dto.CodingDtos.CodingTask;
import ai.nova.platform.coding.dto.CodingDtos.DependencySummary;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.dto.CodingDtos.ParsedCodingOutput;
import ai.nova.platform.coding.security.CodingAuthorizationService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskDependencyRepository;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Production coding agent: receives one orchestration task, calls Agent Runtime for JSON artifacts,
 * validates, and stores them. Never mutates repositories, shell, git, docker, browser, or MCP.
 */
@Service
public class CodingAgentService {

    private final CodingAuthorizationService authorizationService;
    private final CodingProperties properties;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final AgentTaskDependencyRepository dependencyRepository;
    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final AgentRuntimeClient agentRuntimeClient;
    private final CodingPromptBuilder promptBuilder;
    private final CodingJsonParser jsonParser;
    private final CodingArtifactValidator artifactValidator;
    private final ArtifactStorageService artifactStorageService;
    private final AuditRecordingSupport auditRecordingSupport;

    public CodingAgentService(
            CodingAuthorizationService authorizationService,
            CodingProperties properties,
            AgentOrchestrationTaskRepository taskRepository,
            AgentOrchestrationRunRepository runRepository,
            AgentTaskDependencyRepository dependencyRepository,
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            AgentRuntimeClient agentRuntimeClient,
            CodingPromptBuilder promptBuilder,
            CodingJsonParser jsonParser,
            CodingArtifactValidator artifactValidator,
            ArtifactStorageService artifactStorageService,
            AuditRecordingSupport auditRecordingSupport) {
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
        this.dependencyRepository = dependencyRepository;
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.agentRuntimeClient = agentRuntimeClient;
        this.promptBuilder = promptBuilder;
        this.jsonParser = jsonParser;
        this.artifactValidator = artifactValidator;
        this.artifactStorageService = artifactStorageService;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    public CodingResult generate(CodeGenerationRequest request, AuthenticatedUser user) {
        authorizationService.require(user, CodingAuthorizationService.CODING_GENERATE);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CODING_DISABLED", "Coding agent is disabled");
        }

        AgentOrchestrationTask task = requireTask(request.taskId(), user.getOrganizationId());
        publishTaskAudit(user, task, AuditAction.CREATE, AuditResult.SUCCESS, Map.of());
        publishTaskAudit(user, task, AuditAction.START, AuditResult.SUCCESS, Map.of());
        AgentOrchestrationRun run = runRepository
                .findByIdAndOrganizationId(task.getRunId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORCHESTRATION_RUN_NOT_FOUND", "Run not found"));
        Project project = projectRepository
                .findByIdAndOrganizationId(task.getProjectId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));

        CodingPromptContext promptContext = buildContext(task, run, project);
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(promptContext);

        Agent codingAgent = null;
        if (task.getAssignedAgentId() != null) {
            codingAgent = agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            task.getAssignedAgentId(), project.getId(), project.getOrganizationId())
                    .orElse(null);
        }
        String provider = codingAgent != null ? codingAgent.getModelProvider() : properties.getDefaultProvider();
        String model = resolveModel(codingAgent, task);
        if (codingAgent != null
                && codingAgent.getSystemPrompt() != null
                && !codingAgent.getSystemPrompt().isBlank()) {
            systemPrompt = codingAgent.getSystemPrompt().trim() + "\n\n" + systemPrompt;
        }

        ExecutionRequest executionRequest = new ExecutionRequest(
                project.getOrganizationId(),
                project.getId(),
                codingAgent != null ? codingAgent.getId() : null,
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
        try {
            RuntimeTurnResult turn = agentRuntimeClient.execute(executionRequest);
            long generationTimeMs = System.currentTimeMillis() - started;
            if (!turn.isFinal() || turn.finalResponse() == null) {
                publishTaskAudit(
                        user, task, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", "CODING_INVALID_OUTPUT"));
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "CODING_INVALID_OUTPUT",
                        "Coding agent must return a final JSON response without tool calls");
            }
            RuntimeFinalResponse finalResponse = turn.finalResponse();
            ParsedCodingOutput parsed = jsonParser.parse(finalResponse.responseText());
            artifactValidator.validate(parsed);

            Long tokensUsed = (long) finalResponse.totalTokens();
            List<GeneratedArtifactResponse> stored = artifactStorageService.replaceArtifacts(
                    task, parsed.artifacts(), tokensUsed, model, provider, generationTimeMs);

            publishTaskAudit(
                    user,
                    task,
                    AuditAction.COMPLETE,
                    AuditResult.SUCCESS,
                    Map.of("artifactCount", stored.size()));
            return new CodingResult(
                    task.getId(),
                    task.getRunId(),
                    task.getProjectId(),
                    parsed.summary(),
                    stored,
                    tokensUsed,
                    model,
                    provider,
                    generationTimeMs,
                    true);
        } catch (ApiException ex) {
            publishTaskAudit(user, task, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", ex.getCode()));
            throw ex;
        } catch (RuntimeException ex) {
            publishTaskAudit(user, task, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", "CODING_FAILED"));
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<GeneratedArtifactResponse> listArtifacts(UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, CodingAuthorizationService.CODING_READ);
        requireTask(taskId, user.getOrganizationId());
        return artifactStorageService.listByTask(taskId, user.getOrganizationId());
    }

    private CodingPromptContext buildContext(
            AgentOrchestrationTask task, AgentOrchestrationRun run, Project project) {
        List<AgentTaskDependency> deps = dependencyRepository.findByRunId(run.getId());
        Map<UUID, AgentOrchestrationTask> byId = new LinkedHashMap<>();
        for (AgentOrchestrationTask sibling :
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId())) {
            byId.put(sibling.getId(), sibling);
        }
        List<DependencySummary> upstream = new ArrayList<>();
        for (AgentTaskDependency dep : deps) {
            if (!task.getId().equals(dep.getSuccessorTaskId())) {
                continue;
            }
            AgentOrchestrationTask predecessor = byId.get(dep.getPredecessorTaskId());
            if (predecessor == null) {
                continue;
            }
            upstream.add(new DependencySummary(
                    predecessor.getId(),
                    predecessor.getTaskKey(),
                    predecessor.getDisplayName(),
                    predecessor.getStatus().name(),
                    truncate(predecessor.getOutputJson(), 800)));
        }

        Map<String, String> orgSettings = Map.of("organizationId", project.getOrganizationId().toString());
        Map<String, String> projectSettings = new LinkedHashMap<>();
        projectSettings.put("projectId", project.getId().toString());
        projectSettings.put("projectName", project.getName());
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            projectSettings.put("projectDescription", truncate(project.getDescription(), 500));
        }
        projectSettings.put("visibility", project.getVisibility().name());

        return new CodingPromptContext(
                toCodingTask(task),
                run.getObjective() == null ? "" : run.getObjective(),
                List.copyOf(upstream),
                orgSettings,
                projectSettings);
    }

    private AgentOrchestrationTask requireTask(UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CODING_TASK_NOT_FOUND", "Orchestration task not found"));
    }

    private static CodingTask toCodingTask(AgentOrchestrationTask task) {
        return new CodingTask(
                task.getId(),
                task.getOrganizationId(),
                task.getProjectId(),
                task.getRunId(),
                task.getTaskKey(),
                task.getDisplayName(),
                task.getDescription(),
                task.getTaskType().name(),
                task.getStatus().name(),
                task.getInputJson(),
                task.getModelReference(),
                task.getAssignedAgentId());
    }

    private String resolveModel(Agent codingAgent, AgentOrchestrationTask task) {
        if (codingAgent != null && codingAgent.getModelName() != null && !codingAgent.getModelName().isBlank()) {
            return codingAgent.getModelName();
        }
        if (task.getModelReference() != null && !task.getModelReference().isBlank()) {
            return task.getModelReference().trim();
        }
        return properties.getDefaultModel();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
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
                    AuditSource.CODING,
                    details);
        } catch (RuntimeException ignored) {
            // AuditPublisher swallows failures; guard against unexpected propagation.
        }
    }
}
