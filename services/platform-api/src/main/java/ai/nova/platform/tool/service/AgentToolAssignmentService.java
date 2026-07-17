package ai.nova.platform.tool.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.dto.ToolDtos.AgentToolAssignRequest;
import ai.nova.platform.tool.dto.ToolDtos.AgentToolAssignmentResponse;
import ai.nova.platform.tool.entity.AgentToolAssignment;
import ai.nova.platform.tool.entity.ToolDefinition;
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.mapper.ToolMapper;
import ai.nova.platform.tool.repository.AgentToolAssignmentRepository;
import ai.nova.platform.tool.repository.ToolDefinitionRepository;
import ai.nova.platform.tool.security.ToolAuthorizationService;
import ai.nova.platform.web.error.ApiException;

@Service
public class AgentToolAssignmentService {

    private final AgentToolAssignmentRepository assignmentRepository;
    private final ToolDefinitionRepository toolRepository;
    private final AgentRepository agentRepository;
    private final ProjectRepository projectRepository;
    private final ToolMapper toolMapper;
    private final ToolAuthorizationService authorizationService;
    private final ToolAuditService auditService;

    public AgentToolAssignmentService(
            AgentToolAssignmentRepository assignmentRepository,
            ToolDefinitionRepository toolRepository,
            AgentRepository agentRepository,
            ProjectRepository projectRepository,
            ToolMapper toolMapper,
            ToolAuthorizationService authorizationService,
            ToolAuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.toolRepository = toolRepository;
        this.agentRepository = agentRepository;
        this.projectRepository = projectRepository;
        this.toolMapper = toolMapper;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AgentToolAssignmentResponse> list(UUID projectId, UUID agentId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_READ);
        requireProject(projectId, user.getOrganizationId());
        requireAgent(projectId, agentId, user.getOrganizationId());

        List<AgentToolAssignment> assignments = assignmentRepository.findByAgentIdAndProjectIdAndOrganizationIdOrderByCreatedAtAsc(
                agentId, projectId, user.getOrganizationId());
        Map<UUID, ToolDefinition> toolsById = loadTools(assignments, projectId, user.getOrganizationId());

        List<AgentToolAssignmentResponse> responses = new ArrayList<>();
        for (AgentToolAssignment assignment : assignments) {
            ToolDefinition tool = toolsById.get(assignment.getToolId());
            if (tool != null) {
                responses.add(toolMapper.toAssignmentResponse(assignment, tool));
            }
        }
        return responses;
    }

    /**
     * Returns active tool definitions assigned and enabled for an agent.
     * Intended for use by ToolCallingOrchestrator.
     */
    @Transactional(readOnly = true)
    public List<ToolDefinition> loadAssignedActiveTools(UUID projectId, UUID agentId, UUID organizationId) {
        requireProject(projectId, organizationId);
        requireAgent(projectId, agentId, organizationId);

        List<AgentToolAssignment> assignments =
                assignmentRepository.findByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrueOrderByCreatedAtAsc(
                        agentId, projectId, organizationId);
        Map<UUID, ToolDefinition> toolsById = loadTools(assignments, projectId, organizationId);

        return assignments.stream()
                .map(assignment -> toolsById.get(assignment.getToolId()))
                .filter(tool -> tool != null && tool.getStatus() == ToolStatus.ACTIVE)
                .toList();
    }

    @Transactional
    public AgentToolAssignmentResponse assign(
            UUID projectId, UUID agentId, AgentToolAssignRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_ASSIGN);
        requireProject(projectId, user.getOrganizationId());
        requireAgent(projectId, agentId, user.getOrganizationId());

        ToolDefinition tool = toolRepository
                .findByIdAndProjectIdAndOrganizationId(request.toolId(), projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "Tool not found"));
        if (tool.getStatus() != ToolStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "TOOL_NOT_ACTIVE", "Only active tools can be assigned");
        }

        Instant now = Instant.now();
        AgentToolAssignment assignment = assignmentRepository
                .findByAgentIdAndToolIdAndProjectIdAndOrganizationId(
                        agentId, tool.getId(), projectId, user.getOrganizationId())
                .map(existing -> {
                    existing.setEnabled(true);
                    existing.setUpdatedBy(user.getUserId());
                    existing.setUpdatedAt(now);
                    return existing;
                })
                .orElseGet(() -> new AgentToolAssignment(
                        UUID.randomUUID(),
                        user.getOrganizationId(),
                        projectId,
                        agentId,
                        tool.getId(),
                        true,
                        user.getUserId(),
                        now));

        AgentToolAssignment saved = saveAssignment(assignment);
        auditService.toolAssigned(
                user.getOrganizationId(), projectId, tool.getId(), agentId, tool.getToolKey(), user.getUserId());
        return toolMapper.toAssignmentResponse(saved, tool);
    }

    @Transactional
    public void unassign(UUID projectId, UUID agentId, UUID toolId, AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_ASSIGN);
        requireProject(projectId, user.getOrganizationId());
        requireAgent(projectId, agentId, user.getOrganizationId());

        AgentToolAssignment assignment = assignmentRepository
                .findByAgentIdAndToolIdAndProjectIdAndOrganizationId(
                        agentId, toolId, projectId, user.getOrganizationId())
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "TOOL_ASSIGNMENT_NOT_FOUND", "Tool assignment not found"));

        if (!assignment.isEnabled()) {
            return;
        }

        ToolDefinition tool = toolRepository
                .findByIdAndProjectIdAndOrganizationId(toolId, projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "Tool not found"));

        assignment.setEnabled(false);
        assignment.setUpdatedBy(user.getUserId());
        assignment.setUpdatedAt(Instant.now());
        saveAssignment(assignment);
        auditService.toolUnassigned(
                user.getOrganizationId(), projectId, tool.getId(), agentId, tool.getToolKey(), user.getUserId());
    }

    private Map<UUID, ToolDefinition> loadTools(
            List<AgentToolAssignment> assignments, UUID projectId, UUID organizationId) {
        return assignments.stream()
                .map(AgentToolAssignment::getToolId)
                .distinct()
                .map(toolId -> toolRepository.findByIdAndProjectIdAndOrganizationId(toolId, projectId, organizationId))
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(ToolDefinition::getId, Function.identity()));
    }

    private Agent requireAgent(UUID projectId, UUID agentId, UUID organizationId) {
        return agentRepository
                .findByIdAndProjectIdAndOrganizationId(agentId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private AgentToolAssignment saveAssignment(AgentToolAssignment assignment) {
        try {
            return assignmentRepository.saveAndFlush(assignment);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Assignment was modified by another request");
        }
    }
}
