package ai.nova.platform.knowledge.service;

import java.math.BigDecimal;
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
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeAssignRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeAssignmentResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeUpdateRequest;
import ai.nova.platform.knowledge.entity.AgentKnowledgeAssignment;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.mapper.KnowledgeMapper;
import ai.nova.platform.knowledge.repository.AgentKnowledgeAssignmentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeBaseRepository;
import ai.nova.platform.knowledge.security.KnowledgeAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AgentKnowledgeAssignmentService {

    private final AgentKnowledgeAssignmentRepository assignmentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final AgentRepository agentRepository;
    private final ProjectRepository projectRepository;
    private final KnowledgeMapper mapper;
    private final KnowledgeAuthorizationService authorizationService;
    private final KnowledgeProperties properties;

    public AgentKnowledgeAssignmentService(
            AgentKnowledgeAssignmentRepository assignmentRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            AgentRepository agentRepository,
            ProjectRepository projectRepository,
            KnowledgeMapper mapper,
            KnowledgeAuthorizationService authorizationService,
            KnowledgeProperties properties) {
        this.assignmentRepository = assignmentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.agentRepository = agentRepository;
        this.projectRepository = projectRepository;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<AgentKnowledgeAssignmentResponse> list(UUID projectId, UUID agentId, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_READ);
        requireProject(projectId, user.getOrganizationId());
        requireAgent(projectId, agentId, user.getOrganizationId());

        List<AgentKnowledgeAssignment> assignments =
                assignmentRepository.findByAgentIdAndProjectIdAndOrganizationIdOrderByCreatedAtAsc(
                        agentId, projectId, user.getOrganizationId());
        Map<UUID, KnowledgeBase> bases = loadBases(assignments, projectId, user.getOrganizationId());
        List<AgentKnowledgeAssignmentResponse> responses = new ArrayList<>();
        for (AgentKnowledgeAssignment assignment : assignments) {
            KnowledgeBase kb = bases.get(assignment.getKnowledgeBaseId());
            if (kb != null) {
                responses.add(mapper.toAssignmentResponse(assignment, kb));
            }
        }
        return responses;
    }

    @Transactional
    public AgentKnowledgeAssignmentResponse assign(
            UUID projectId, UUID agentId, AgentKnowledgeAssignRequest request, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_ASSIGN);
        requireProject(projectId, user.getOrganizationId());
        Agent agent = requireAgent(projectId, agentId, user.getOrganizationId());
        if (agent.getStatus() == AgentStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "AGENT_ARCHIVED", "Agent is archived");
        }
        KnowledgeBase kb = knowledgeBaseRepository
                .findByIdAndProjectIdAndOrganizationId(
                        request.knowledgeBaseId(), projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "KNOWLEDGE_BASE_NOT_FOUND", "Knowledge base not found"));
        if (kb.getStatus() == KnowledgeBaseStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "KNOWLEDGE_ARCHIVED", "Knowledge base is archived");
        }
        validateOverrides(request.topKOverride(), request.minimumScoreOverride());

        return assignmentRepository
                .findByAgentIdAndKnowledgeBaseIdAndProjectIdAndOrganizationId(
                        agentId, request.knowledgeBaseId(), projectId, user.getOrganizationId())
                .map(existing -> {
                    existing.setEnabled(true);
                    existing.setTopKOverride(request.topKOverride());
                    existing.setMinimumScoreOverride(request.minimumScoreOverride());
                    existing.setUpdatedBy(user.getUserId());
                    existing.setUpdatedAt(Instant.now());
                    return mapper.toAssignmentResponse(assignmentRepository.save(existing), kb);
                })
                .orElseGet(() -> {
                    AgentKnowledgeAssignment created = new AgentKnowledgeAssignment(
                            UUID.randomUUID(),
                            user.getOrganizationId(),
                            projectId,
                            agentId,
                            request.knowledgeBaseId(),
                            true,
                            request.topKOverride(),
                            request.minimumScoreOverride(),
                            user.getUserId(),
                            Instant.now());
                    return mapper.toAssignmentResponse(assignmentRepository.save(created), kb);
                });
    }

    @Transactional
    public AgentKnowledgeAssignmentResponse update(
            UUID projectId,
            UUID agentId,
            UUID knowledgeBaseId,
            AgentKnowledgeUpdateRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_ASSIGN);
        requireProject(projectId, user.getOrganizationId());
        requireAgent(projectId, agentId, user.getOrganizationId());
        KnowledgeBase kb = knowledgeBaseRepository
                .findByIdAndProjectIdAndOrganizationId(knowledgeBaseId, projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "KNOWLEDGE_BASE_NOT_FOUND", "Knowledge base not found"));
        AgentKnowledgeAssignment assignment = assignmentRepository
                .findByAgentIdAndKnowledgeBaseIdAndProjectIdAndOrganizationId(
                        agentId, knowledgeBaseId, projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "KNOWLEDGE_ASSIGNMENT_NOT_FOUND", "Assignment not found"));
        if (!assignment.getVersion().equals(request.version())) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Assignment was updated concurrently");
        }
        validateOverrides(request.topKOverride(), request.minimumScoreOverride());
        if (request.enabled() != null) {
            assignment.setEnabled(request.enabled());
        }
        if (request.topKOverride() != null || request.minimumScoreOverride() != null) {
            assignment.setTopKOverride(request.topKOverride());
            assignment.setMinimumScoreOverride(request.minimumScoreOverride());
        }
        assignment.setUpdatedBy(user.getUserId());
        assignment.setUpdatedAt(Instant.now());
        try {
            return mapper.toAssignmentResponse(assignmentRepository.save(assignment), kb);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Assignment was updated concurrently");
        }
    }

    @Transactional
    public void unassign(UUID projectId, UUID agentId, UUID knowledgeBaseId, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_ASSIGN);
        requireProject(projectId, user.getOrganizationId());
        requireAgent(projectId, agentId, user.getOrganizationId());
        assignmentRepository
                .findByAgentIdAndKnowledgeBaseIdAndProjectIdAndOrganizationId(
                        agentId, knowledgeBaseId, projectId, user.getOrganizationId())
                .ifPresent(assignment -> {
                    assignment.setEnabled(false);
                    assignment.setUpdatedBy(user.getUserId());
                    assignment.setUpdatedAt(Instant.now());
                    assignmentRepository.save(assignment);
                });
    }

    private void validateOverrides(Integer topKOverride, BigDecimal minimumScoreOverride) {
        if (topKOverride != null && (topKOverride < 1 || topKOverride > properties.getMaximumTopK())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "topKOverride out of range");
        }
        if (minimumScoreOverride != null
                && (minimumScoreOverride.compareTo(BigDecimal.valueOf(-1)) < 0
                        || minimumScoreOverride.compareTo(BigDecimal.ONE) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "minimumScoreOverride out of range");
        }
    }

    private Map<UUID, KnowledgeBase> loadBases(
            List<AgentKnowledgeAssignment> assignments, UUID projectId, UUID organizationId) {
        List<UUID> ids = assignments.stream().map(AgentKnowledgeAssignment::getKnowledgeBaseId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return knowledgeBaseRepository.findByIdInAndProjectIdAndOrganizationId(ids, projectId, organizationId).stream()
                .collect(Collectors.toMap(KnowledgeBase::getId, Function.identity()));
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
}
