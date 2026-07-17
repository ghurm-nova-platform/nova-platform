package ai.nova.platform.execution.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.ExecutionResult;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteRequest;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteResponse;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecutionDetailResponse;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecutionSummaryResponse;
import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionMessage;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.mapper.ExecutionMapper;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.execution.repository.ExecutionMessageRepository;
import ai.nova.platform.execution.security.ExecutionAuthorizationService;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.prompt.entity.Prompt;
import ai.nova.platform.prompt.entity.PromptStatus;
import ai.nova.platform.prompt.entity.PromptVariable;
import ai.nova.platform.prompt.entity.PromptVersion;
import ai.nova.platform.prompt.entity.PromptVersionStatus;
import ai.nova.platform.prompt.parser.PromptVariableParser;
import ai.nova.platform.prompt.parser.PromptVariableParser.PreviewResult;
import ai.nova.platform.prompt.parser.PromptVariableParser.VariableDefinition;
import ai.nova.platform.prompt.repository.PromptRepository;
import ai.nova.platform.prompt.repository.PromptVariableRepository;
import ai.nova.platform.prompt.repository.PromptVersionRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final AgentExecutionRepository executionRepository;
    private final ExecutionMessageRepository messageRepository;
    private final AgentRepository agentRepository;
    private final PromptRepository promptRepository;
    private final PromptVersionRepository promptVersionRepository;
    private final PromptVariableRepository promptVariableRepository;
    private final ProjectRepository projectRepository;
    private final ExecutionMapper executionMapper;
    private final ExecutionAuthorizationService authorizationService;
    private final PromptVariableParser variableParser;
    private final AgentRuntimeClient agentRuntimeClient;
    private final ExecutionLifecycleService lifecycleService;

    public ExecutionService(
            AgentExecutionRepository executionRepository,
            ExecutionMessageRepository messageRepository,
            AgentRepository agentRepository,
            PromptRepository promptRepository,
            PromptVersionRepository promptVersionRepository,
            PromptVariableRepository promptVariableRepository,
            ProjectRepository projectRepository,
            ExecutionMapper executionMapper,
            ExecutionAuthorizationService authorizationService,
            PromptVariableParser variableParser,
            AgentRuntimeClient agentRuntimeClient,
            ExecutionLifecycleService lifecycleService) {
        this.executionRepository = executionRepository;
        this.messageRepository = messageRepository;
        this.agentRepository = agentRepository;
        this.promptRepository = promptRepository;
        this.promptVersionRepository = promptVersionRepository;
        this.promptVariableRepository = promptVariableRepository;
        this.projectRepository = projectRepository;
        this.executionMapper = executionMapper;
        this.authorizationService = authorizationService;
        this.variableParser = variableParser;
        this.agentRuntimeClient = agentRuntimeClient;
        this.lifecycleService = lifecycleService;
    }

    /**
     * Orchestrates execution outside a single long transaction so RUNNING is committed
     * before the runtime call and concurrent cancel can win the race.
     */
    public ExecuteResponse execute(
            UUID projectId, UUID agentId, ExecuteRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ExecutionAuthorizationService.AGENT_EXECUTE);
        requireProjectInOrganization(projectId, user.getOrganizationId());

        Agent agent = requireAgent(projectId, agentId, user.getOrganizationId());
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "AGENT_NOT_ACTIVE", "Agent is not active");
        }

        ResolvedPrompt resolved = resolvePromptVersion(agent, user.getOrganizationId(), projectId);
        List<VariableDefinition> variableDefs = loadVariableDefinitions(resolved.versionId());
        PreviewResult preview = variableParser.preview(
                resolved.content(), request.variables(), variableDefs);
        if (!preview.missingRequiredVariables().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "MISSING_VARIABLE",
                    "Missing required variables: " + String.join(", ", preview.missingRequiredVariables()));
        }
        if (!preview.errors().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_INPUT", preview.errors().getFirst());
        }

        String renderedPrompt = preview.renderedContent();
        String userMessage = request.input().message().trim();
        UUID executionId = UUID.randomUUID();

        // Transaction 1: persist RUNNING and commit before runtime.
        lifecycleService.startRunning(
                executionId,
                user.getOrganizationId(),
                projectId,
                agentId,
                resolved.versionId(),
                request.conversationId(),
                agent.getModelProvider(),
                agent.getModelName(),
                user.getUserId(),
                renderedPrompt,
                userMessage);

        log.debug("Starting execution {} for agent {} in project {}", executionId, agentId, projectId);

        try {
            // Outside transaction: runtime call (may be cancelled concurrently).
            ExecutionResult result = agentRuntimeClient.execute(new ExecutionRequest(
                    user.getOrganizationId(),
                    projectId,
                    agentId,
                    executionId,
                    agent.getModelProvider(),
                    agent.getModelName(),
                    renderedPrompt,
                    userMessage,
                    request.conversationId()));

            // Transaction 2: complete only if still RUNNING (not CANCELLED).
            AgentExecution completed = lifecycleService.completeIfRunning(executionId, result);
            String responseText = completed.getStatus() == ExecutionStatus.COMPLETED
                    ? result.responseText()
                    : null;
            return executionMapper.toExecuteResponse(completed, responseText, renderedPrompt);
        } catch (RuntimeException ex) {
            // Never persist exception text — may contain provider secrets or request payloads.
            log.warn(
                    "Execution {} failed for agent {} (details omitted from persistence)",
                    executionId,
                    agentId);
            AgentExecution failed = lifecycleService.failIfRunning(executionId);
            return executionMapper.toExecuteResponse(
                    failed, null, renderedPrompt);
        }
    }

    public ExecutionDetailResponse cancel(UUID projectId, UUID executionId, AuthenticatedUser user) {
        authorizationService.require(user, ExecutionAuthorizationService.EXECUTION_CANCEL);
        requireProjectInOrganization(projectId, user.getOrganizationId());

        AgentExecution cancelled = lifecycleService.cancelIfActive(
                projectId, executionId, user.getOrganizationId());
        try {
            agentRuntimeClient.cancel(executionId);
        } catch (RuntimeException ex) {
            log.debug("Runtime cancel notification failed for execution {}", executionId);
        }
        return toDetail(cancelled);
    }

    @Transactional(readOnly = true)
    public Page<ExecutionSummaryResponse> list(
            UUID projectId,
            AuthenticatedUser user,
            UUID agentId,
            ExecutionStatus status,
            Pageable pageable) {
        authorizationService.require(user, ExecutionAuthorizationService.EXECUTION_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        return executionRepository
                .search(user.getOrganizationId(), projectId, agentId, status, pageable)
                .map(executionMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ExecutionDetailResponse get(UUID projectId, UUID executionId, AuthenticatedUser user) {
        authorizationService.require(user, ExecutionAuthorizationService.EXECUTION_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        return toDetail(requireExecution(projectId, executionId, user.getOrganizationId()));
    }

    private ExecutionDetailResponse toDetail(AgentExecution execution) {
        List<ExecutionMessage> messages =
                messageRepository.findByExecutionIdOrderByCreatedAtAsc(execution.getId());
        return executionMapper.toDetail(execution, messages);
    }

    private record ResolvedPrompt(UUID versionId, String content) {
    }

    private ResolvedPrompt resolvePromptVersion(Agent agent, UUID organizationId, UUID projectId) {
        UUID promptVersionId = agent.getPromptVersionId();
        UUID promptId = agent.getPromptId();

        if (promptVersionId == null && promptId == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Agent has no published prompt configured");
        }

        Prompt prompt = null;
        if (promptId != null) {
            prompt = promptRepository
                    .findByIdAndProjectIdAndOrganizationId(promptId, projectId, organizationId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Agent prompt is not available"));
            if (prompt.getStatus() == PromptStatus.ARCHIVED) {
                throw new ApiException(HttpStatus.CONFLICT, "PROMPT_ARCHIVED", "Prompt is archived");
            }
        }

        if (promptVersionId == null) {
            promptVersionId = prompt.getPublishedVersionId();
            if (promptVersionId == null) {
                throw new ApiException(
                        HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Prompt has no published version");
            }
        }

        UUID resolvedPromptId = promptId;
        if (resolvedPromptId == null) {
            PromptVersion versionForPromptId = promptVersionRepository
                    .findById(promptVersionId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Prompt version is not available"));
            resolvedPromptId = versionForPromptId.getPromptId();
            if (prompt == null) {
                prompt = promptRepository
                        .findByIdAndProjectIdAndOrganizationId(resolvedPromptId, projectId, organizationId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Agent prompt is not available"));
                if (prompt.getStatus() == PromptStatus.ARCHIVED) {
                    throw new ApiException(HttpStatus.CONFLICT, "PROMPT_ARCHIVED", "Prompt is archived");
                }
            }
        }

        final UUID finalPromptId = resolvedPromptId;
        final UUID finalVersionId = promptVersionId;
        PromptVersion version = promptVersionRepository
                .findByIdAndPromptIdAndOrganizationIdAndProjectId(
                        finalVersionId, finalPromptId, organizationId, projectId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Prompt version is not available"));

        if (version.getStatus() == PromptVersionStatus.DRAFT) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Prompt version is not published");
        }
        if (version.getStatus() != PromptVersionStatus.PUBLISHED
                && version.getStatus() != PromptVersionStatus.SUPERSEDED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "PROMPT_NOT_PUBLISHED", "Prompt version is not published");
        }

        return new ResolvedPrompt(version.getId(), version.getContent());
    }

    private List<VariableDefinition> loadVariableDefinitions(UUID versionId) {
        return promptVariableRepository.findByPromptVersionIdOrderByNameAsc(versionId).stream()
                .map(this::toDefinition)
                .toList();
    }

    private VariableDefinition toDefinition(PromptVariable variable) {
        return new VariableDefinition(variable.getName(), variable.isRequired(), variable.getDefaultValue());
    }

    private Agent requireAgent(UUID projectId, UUID agentId, UUID organizationId) {
        return agentRepository
                .findByIdAndProjectIdAndOrganizationId(agentId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
    }

    private AgentExecution requireExecution(UUID projectId, UUID executionId, UUID organizationId) {
        return executionRepository
                .findByIdAndProjectIdAndOrganizationId(executionId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Execution not found"));
    }

    private Project requireProjectInOrganization(UUID projectId, UUID organizationId) {
        return projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }
}
