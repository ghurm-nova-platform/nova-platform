package ai.nova.platform.agent.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.dto.AgentDtos.AgentCreateRequest;
import ai.nova.platform.agent.dto.AgentDtos.AgentResponse;
import ai.nova.platform.agent.dto.AgentDtos.AgentStatusRequest;
import ai.nova.platform.agent.dto.AgentDtos.AgentUpdateRequest;
import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.entity.AgentAuditAction;
import ai.nova.platform.agent.entity.AgentAuditLog;
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.mapper.AgentMapper;
import ai.nova.platform.agent.repository.AgentAuditLogRepository;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.validation.ModelProviderAllowlist;
import ai.nova.platform.prompt.service.PromptService;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentAuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final AgentMapper agentMapper;
    private final AgentAuthorizationService authorizationService;
    private final ModelProviderAllowlist modelProviderAllowlist;
    private final AgentRuntimeClient agentRuntimeClient;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    public AgentService(
            AgentRepository agentRepository,
            AgentAuditLogRepository auditLogRepository,
            ProjectRepository projectRepository,
            AgentMapper agentMapper,
            AgentAuthorizationService authorizationService,
            ModelProviderAllowlist modelProviderAllowlist,
            AgentRuntimeClient agentRuntimeClient,
            ObjectMapper objectMapper,
            PromptService promptService) {
        this.agentRepository = agentRepository;
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.agentMapper = agentMapper;
        this.authorizationService = authorizationService;
        this.modelProviderAllowlist = modelProviderAllowlist;
        this.agentRuntimeClient = agentRuntimeClient;
        this.objectMapper = objectMapper;
        this.promptService = promptService;
    }

    @Transactional(readOnly = true)
    public Page<AgentResponse> list(
            UUID projectId,
            AuthenticatedUser user,
            String search,
            AgentStatus status,
            Pageable pageable) {
        authorizationService.require(user, AgentAuthorizationService.AGENT_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        return agentRepository
                .search(user.getOrganizationId(), projectId, normalize(search), status, pageable)
                .map(agentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AgentResponse get(UUID projectId, UUID agentId, AuthenticatedUser user) {
        authorizationService.require(user, AgentAuthorizationService.AGENT_READ);
        return agentMapper.toResponse(requireAgent(projectId, agentId, user.getOrganizationId()));
    }

    @Transactional
    public AgentResponse create(UUID projectId, AgentCreateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, AgentAuthorizationService.AGENT_CREATE);
        Project project = requireProjectInOrganization(projectId, user.getOrganizationId());

        String name = request.name().trim();
        if (agentRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "AGENT_NAME_EXISTS", "Agent name already exists in this project");
        }

        String provider = modelProviderAllowlist.requireAllowed(request.modelProvider());
        Instant now = Instant.now();
        Agent agent = new Agent(
                UUID.randomUUID(),
                user.getOrganizationId(),
                project.getId(),
                name,
                trimToNull(request.description()),
                request.systemPrompt().trim(),
                provider,
                request.modelName().trim(),
                request.temperature(),
                request.maxTokens(),
                AgentStatus.DRAFT,
                request.visibility(),
                user.getUserId(),
                now);
        applyPromptReference(agent, request.promptId(), request.promptVersionId(), user.getOrganizationId(), project.getId());
        Agent saved = agentRepository.save(agent);
        writeAudit(saved, AgentAuditAction.CREATED, null, snapshot(saved), user.getUserId());
        safelySyncCreateOrUpdate(saved);
        return agentMapper.toResponse(saved);
    }

    @Transactional
    public AgentResponse update(UUID projectId, UUID agentId, AgentUpdateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, AgentAuthorizationService.AGENT_UPDATE);
        Agent agent = requireAgent(projectId, agentId, user.getOrganizationId());
        assertVersion(agent, request.version());

        String name = request.name().trim();
        if (agentRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(projectId, name, agentId)) {
            throw new ApiException(HttpStatus.CONFLICT, "AGENT_NAME_EXISTS", "Agent name already exists in this project");
        }
        if (agent.getStatus() == AgentStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_AGENT_STATUS", "Archived agents cannot be updated");
        }

        String oldSnapshot = snapshot(agent);
        agent.setName(name);
        agent.setDescription(trimToNull(request.description()));
        agent.setSystemPrompt(request.systemPrompt().trim());
        agent.setModelProvider(modelProviderAllowlist.requireAllowed(request.modelProvider()));
        agent.setModelName(request.modelName().trim());
        agent.setTemperature(request.temperature());
        agent.setMaxTokens(request.maxTokens());
        agent.setVisibility(request.visibility());
        applyPromptReference(agent, request.promptId(), request.promptVersionId(), user.getOrganizationId(), projectId);
        agent.setUpdatedBy(user.getUserId());
        agent.setUpdatedAt(Instant.now());

        Agent saved = saveWithOptimisticLock(agent);
        writeAudit(saved, AgentAuditAction.UPDATED, oldSnapshot, snapshot(saved), user.getUserId());
        safelySyncCreateOrUpdate(saved);
        return agentMapper.toResponse(saved);
    }

    @Transactional
    public AgentResponse updateStatus(
            UUID projectId, UUID agentId, AgentStatusRequest request, AuthenticatedUser user) {
        Agent agent = requireAgent(projectId, agentId, user.getOrganizationId());
        assertVersion(agent, request.version());

        AgentStatus target = request.status();
        if (target == AgentStatus.ARCHIVED) {
            authorizationService.require(user, AgentAuthorizationService.AGENT_ARCHIVE);
        } else if (target == AgentStatus.ACTIVE || target == AgentStatus.PAUSED) {
            authorizationService.require(user, AgentAuthorizationService.AGENT_ACTIVATE);
        } else if (target == AgentStatus.DRAFT) {
            authorizationService.require(user, AgentAuthorizationService.AGENT_UPDATE);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AGENT_STATUS", "Unsupported status transition");
        }

        if (agent.getStatus() == AgentStatus.ARCHIVED && target != AgentStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_AGENT_STATUS", "Archived agents cannot change status");
        }

        String oldSnapshot = snapshot(agent);
        AgentAuditAction action = switch (target) {
            case ACTIVE -> AgentAuditAction.ACTIVATED;
            case PAUSED -> AgentAuditAction.PAUSED;
            case ARCHIVED -> AgentAuditAction.ARCHIVED;
            case DRAFT -> AgentAuditAction.UPDATED;
        };
        agent.setStatus(target);
        agent.setUpdatedBy(user.getUserId());
        agent.setUpdatedAt(Instant.now());
        Agent saved = saveWithOptimisticLock(agent);
        writeAudit(saved, action, oldSnapshot, snapshot(saved), user.getUserId());
        if (target == AgentStatus.ARCHIVED) {
            safelyArchive(saved);
        } else {
            safelySyncCreateOrUpdate(saved);
        }
        return agentMapper.toResponse(saved);
    }

    @Transactional
    public void archive(UUID projectId, UUID agentId, AuthenticatedUser user) {
        authorizationService.require(user, AgentAuthorizationService.AGENT_ARCHIVE);
        Agent agent = requireAgent(projectId, agentId, user.getOrganizationId());
        if (agent.getStatus() == AgentStatus.ACTIVE) {
            // Spec: do not physically delete active agents — archive instead.
        }
        if (agent.getStatus() == AgentStatus.ARCHIVED) {
            return;
        }
        String oldSnapshot = snapshot(agent);
        agent.setStatus(AgentStatus.ARCHIVED);
        agent.setUpdatedBy(user.getUserId());
        agent.setUpdatedAt(Instant.now());
        Agent saved = saveWithOptimisticLock(agent);
        writeAudit(saved, AgentAuditAction.ARCHIVED, oldSnapshot, snapshot(saved), user.getUserId());
        safelyArchive(saved);
    }

    private Agent requireAgent(UUID projectId, UUID agentId, UUID organizationId) {
        requireProjectInOrganization(projectId, organizationId);
        return agentRepository
                .findByIdAndProjectIdAndOrganizationId(agentId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
    }

    private Project requireProjectInOrganization(UUID projectId, UUID organizationId) {
        return projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void assertVersion(Agent agent, Integer version) {
        if (version == null || !version.equals(agent.getVersion())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "OPTIMISTIC_LOCK_CONFLICT",
                    "Agent was modified by another request");
        }
    }

    private Agent saveWithOptimisticLock(Agent agent) {
        try {
            return agentRepository.saveAndFlush(agent);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "OPTIMISTIC_LOCK_CONFLICT",
                    "Agent was modified by another request");
        }
    }

    private void writeAudit(
            Agent agent,
            AgentAuditAction action,
            String oldValue,
            String newValue,
            UUID performedBy) {
        auditLogRepository.save(new AgentAuditLog(
                UUID.randomUUID(),
                agent.getId(),
                agent.getOrganizationId(),
                agent.getProjectId(),
                action,
                oldValue,
                newValue,
                performedBy,
                Instant.now()));
    }

    private void safelySyncCreateOrUpdate(Agent agent) {
        try {
            agentRuntimeClient.createOrUpdateAgentDefinition(
                    agent.getOrganizationId(),
                    agent.getProjectId(),
                    agent.getId(),
                    agent.getName(),
                    agent.getStatus().name());
        } catch (RuntimeException ignored) {
            // Runtime sync must not roll back the DB transaction in this phase.
        }
    }

    private void safelyArchive(Agent agent) {
        try {
            agentRuntimeClient.archiveAgentDefinition(
                    agent.getOrganizationId(), agent.getProjectId(), agent.getId());
        } catch (RuntimeException ignored) {
            // no-op
        }
    }

    private String snapshot(Agent agent) {
        try {
            return objectMapper.writeValueAsString(agentMapper.toResponse(agent));
        } catch (JsonProcessingException ex) {
            return agent.getId().toString();
        }
    }

    private String normalize(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void applyPromptReference(
            Agent agent, UUID promptId, UUID promptVersionId, UUID organizationId, UUID projectId) {
        if (promptId == null && promptVersionId == null) {
            agent.setPromptId(null);
            agent.setPromptVersionId(null);
            return;
        }
        if (promptId == null || promptVersionId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PROMPT_REFERENCE",
                    "Both promptId and promptVersionId must be provided together");
        }
        promptService.requirePublishedPromptReference(organizationId, projectId, promptId, promptVersionId);
        agent.setPromptId(promptId);
        agent.setPromptVersionId(promptVersionId);
    }
}
