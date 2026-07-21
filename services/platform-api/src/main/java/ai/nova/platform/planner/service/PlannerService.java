package ai.nova.platform.planner.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import ai.nova.platform.planner.config.PlannerProperties;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionEstimate;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerRequest;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerResponse;
import ai.nova.platform.planner.entity.PlannerTemplate;
import ai.nova.platform.planner.repository.PlannerTemplateRepository;
import ai.nova.platform.planner.security.PlannerAuthorizationService;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Produces validated execution plans. Never starts orchestration execution.
 */
@Service
public class PlannerService {

    private final PlannerAuthorizationService authorizationService;
    private final ProjectRepository projectRepository;
    private final PlannerTemplateRepository templateRepository;
    private final AgentRepository agentRepository;
    private final AgentRuntimeClient agentRuntimeClient;
    private final PlannerPromptBuilder promptBuilder;
    private final PlannerJsonParser jsonParser;
    private final PlannerPlanValidator planValidator;
    private final PlannerEstimationService estimationService;
    private final PlannerProperties properties;
    private final ObjectMapper objectMapper;
    private final AuditRecordingSupport auditRecordingSupport;

    public PlannerService(
            PlannerAuthorizationService authorizationService,
            ProjectRepository projectRepository,
            PlannerTemplateRepository templateRepository,
            AgentRepository agentRepository,
            AgentRuntimeClient agentRuntimeClient,
            PlannerPromptBuilder promptBuilder,
            PlannerJsonParser jsonParser,
            PlannerPlanValidator planValidator,
            PlannerEstimationService estimationService,
            PlannerProperties properties,
            ObjectMapper objectMapper,
            AuditRecordingSupport auditRecordingSupport) {
        this.authorizationService = authorizationService;
        this.projectRepository = projectRepository;
        this.templateRepository = templateRepository;
        this.agentRepository = agentRepository;
        this.agentRuntimeClient = agentRuntimeClient;
        this.promptBuilder = promptBuilder;
        this.jsonParser = jsonParser;
        this.planValidator = planValidator;
        this.estimationService = estimationService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    public PlannerResponse plan(PlannerRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_PLAN);
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PLANNER_DISABLED", "Planner is disabled");
        }
        Project project = requireProject(request.projectId(), user.getOrganizationId());
        UUID planOperationId = UUID.randomUUID();
        publishAudit(user, project, planOperationId, AuditAction.CREATE, AuditResult.SUCCESS, Map.of());
        String objective = request.objective().trim();
        if (objective.length() > properties.getMaxObjectiveLength()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PLANNER_OBJECTIVE_TOO_LONG", "Objective exceeds maximum length");
        }

        PlannerTemplate template = resolveTemplate(request.templateId(), project);
        String metadataJson = resolveMetadataJson(request);
        String systemPrompt = promptBuilder.buildSystemPrompt(template);
        String userPrompt = promptBuilder.buildUserPrompt(project, objective, metadataJson);

        Agent plannerAgent = null;
        if (request.plannerAgentId() != null) {
            plannerAgent = agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            request.plannerAgentId(), project.getId(), project.getOrganizationId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Planner agent not found"));
        }

        String provider = plannerAgent != null ? plannerAgent.getModelProvider() : properties.getDefaultProvider();
        String model = plannerAgent != null ? plannerAgent.getModelName() : properties.getDefaultModel();
        if (plannerAgent != null && plannerAgent.getSystemPrompt() != null && !plannerAgent.getSystemPrompt().isBlank()) {
            systemPrompt = plannerAgent.getSystemPrompt().trim() + "\n\n" + systemPrompt;
        }

        ExecutionRequest executionRequest = new ExecutionRequest(
                project.getOrganizationId(),
                project.getId(),
                plannerAgent != null ? plannerAgent.getId() : null,
                UUID.randomUUID(),
                provider,
                model,
                systemPrompt,
                List.of(new RuntimeMessage("USER", userPrompt)),
                null,
                List.of(),
                List.of(),
                null);

        // External AI call — no open DB write transaction around provider invocation.
        try {
            RuntimeTurnResult turn = agentRuntimeClient.execute(executionRequest);
            if (!turn.isFinal() || turn.finalResponse() == null) {
                publishAudit(
                        user,
                        project,
                        planOperationId,
                        AuditAction.FAIL,
                        AuditResult.FAILURE,
                        Map.of("errorCode", "PLANNER_INVALID_OUTPUT"));
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PLANNER_INVALID_OUTPUT",
                        "Planner must return a final JSON response without tool calls");
            }
            RuntimeFinalResponse finalResponse = turn.finalResponse();
            ExecutionPlan plan = jsonParser.parse(finalResponse.responseText(), objective);
            planValidator.validate(plan);
            ExecutionEstimate estimate = estimationService.estimate(plan);
            ExecutionPlan enriched = new ExecutionPlan(
                    plan.objective(),
                    plan.executionMode(),
                    plan.failurePolicy(),
                    plan.maxParallelTasks(),
                    plan.maximumDurationMs(),
                    estimate.complexity(),
                    estimate.estimatedTokens(),
                    estimate.estimatedDurationSeconds(),
                    estimate.estimatedCostUsd(),
                    estimate.riskLevel(),
                    plan.tasks(),
                    plan.dependencies(),
                    plan.metadata());

            publishAudit(
                    user,
                    project,
                    planOperationId,
                    AuditAction.COMPLETE,
                    AuditResult.SUCCESS,
                    Map.of("taskCount", enriched.tasks() == null ? 0 : enriched.tasks().size()));
            return new PlannerResponse(
                    enriched,
                    estimate,
                    sanitizeRaw(finalResponse.responseText()),
                    template != null ? template.getId() : null,
                    true);
        } catch (ApiException ex) {
            publishAudit(
                    user,
                    project,
                    planOperationId,
                    AuditAction.FAIL,
                    AuditResult.FAILURE,
                    Map.of("errorCode", ex.getCode()));
            throw ex;
        } catch (RuntimeException ex) {
            publishAudit(
                    user,
                    project,
                    planOperationId,
                    AuditAction.FAIL,
                    AuditResult.FAILURE,
                    Map.of("errorCode", "PLANNER_FAILED"));
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<PlannerTemplate> listTemplates(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_TEMPLATE_READ);
        requireProject(projectId, user.getOrganizationId());
        return templateRepository.findEnabledForProject(user.getOrganizationId(), projectId);
    }

    private PlannerTemplate resolveTemplate(UUID templateId, Project project) {
        if (templateId != null) {
            return templateRepository
                    .findByIdAndOrganizationId(templateId, project.getOrganizationId())
                    .filter(PlannerTemplate::isEnabled)
                    .filter(t -> t.getProjectId() == null || t.getProjectId().equals(project.getId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLANNER_TEMPLATE_NOT_FOUND", "Template not found"));
        }
        List<PlannerTemplate> templates =
                templateRepository.findEnabledForProject(project.getOrganizationId(), project.getId());
        return templates.isEmpty() ? null : templates.get(0);
    }

    private String resolveMetadataJson(PlannerRequest request) {
        if (request.metadataJson() != null && !request.metadataJson().isBlank()) {
            return request.metadataJson().trim();
        }
        if (request.metadata() != null && !request.metadata().isEmpty()) {
            try {
                return objectMapper.writeValueAsString(request.metadata());
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PLANNER_METADATA_INVALID", "metadata is invalid");
            }
        }
        return "{}";
    }

    private Project requireProject(UUID projectId, UUID organizationId) {
        return projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private static String sanitizeRaw(String raw) {
        if (raw == null) {
            return null;
        }
        // Bound stored/returned raw planner text for clients.
        return raw.length() > 50000 ? raw.substring(0, 50000) : raw;
    }

    private void publishAudit(
            AuthenticatedUser user,
            Project project,
            UUID operationId,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        try {
            auditRecordingSupport.recordDomainEvent(
                    user,
                    project.getId(),
                    AuditEntityType.CONFIGURATION,
                    operationId,
                    "planner-plan",
                    action,
                    result,
                    AuditSource.PLANNER,
                    details);
        } catch (RuntimeException ignored) {
            // AuditPublisher swallows failures; guard against unexpected propagation.
        }
    }
}
