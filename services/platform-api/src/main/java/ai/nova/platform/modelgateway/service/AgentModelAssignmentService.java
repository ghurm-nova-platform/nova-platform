package ai.nova.platform.modelgateway.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.AgentModelAssignmentResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.AssignAgentModelRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateAgentModelAssignmentRequest;
import ai.nova.platform.modelgateway.entity.AgentModelAssignment;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.ProjectModel;
import ai.nova.platform.modelgateway.mapper.ModelGatewayMapper;
import ai.nova.platform.modelgateway.repository.AgentModelAssignmentRepository;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.repository.ProjectModelRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AgentModelAssignmentService {

    private final AgentModelAssignmentRepository assignmentRepository;
    private final AiModelRepository modelRepository;
    private final AiProviderRepository providerRepository;
    private final ProjectModelRepository projectModelRepository;
    private final AgentRepository agentRepository;
    private final ProjectRepository projectRepository;
    private final ModelGatewayMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;

    public AgentModelAssignmentService(
            AgentModelAssignmentRepository assignmentRepository,
            AiModelRepository modelRepository,
            AiProviderRepository providerRepository,
            ProjectModelRepository projectModelRepository,
            AgentRepository agentRepository,
            ProjectRepository projectRepository,
            ModelGatewayMapper mapper,
            ModelGatewayAuthorizationService authorizationService) {
        this.assignmentRepository = assignmentRepository;
        this.modelRepository = modelRepository;
        this.providerRepository = providerRepository;
        this.projectModelRepository = projectModelRepository;
        this.agentRepository = agentRepository;
        this.projectRepository = projectRepository;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<AgentModelAssignmentResponse> list(UUID projectId, UUID agentId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_READ);
        requireContext(projectId, agentId, user.getOrganizationId());
        List<AgentModelAssignment> assignments = assignmentRepository
                .findByAgentIdAndProjectIdAndOrganizationIdOrderByAssignmentRoleAscPriorityAscModelIdAsc(
                        agentId, projectId, user.getOrganizationId());
        return toResponses(assignments, user.getOrganizationId());
    }

    @Transactional
    public AgentModelAssignmentResponse assign(
            UUID projectId, UUID agentId, AssignAgentModelRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_AGENT_ASSIGN);
        requireContext(projectId, agentId, user.getOrganizationId());
        requireProjectModelEnabled(projectId, request.modelId(), user.getOrganizationId());
        AiModel model = requireActiveModel(request.modelId(), user.getOrganizationId());

        if (assignmentRepository.existsByAgentIdAndAssignmentRoleAndPriorityAndProjectIdAndOrganizationId(
                agentId, request.assignmentRole(), request.priority(), projectId, user.getOrganizationId())) {
            throw new ApiException(HttpStatus.CONFLICT, "ASSIGNMENT_EXISTS", "Priority already used for role");
        }

        Instant now = Instant.now();
        AgentModelAssignment assignment = assignmentRepository
                .findByAgentIdAndModelIdAndProjectIdAndOrganizationId(
                        agentId, model.getId(), projectId, user.getOrganizationId())
                .orElseGet(() -> {
                    AgentModelAssignment created = new AgentModelAssignment();
                    created.setId(UUID.randomUUID());
                    created.setOrganizationId(user.getOrganizationId());
                    created.setProjectId(projectId);
                    created.setAgentId(agentId);
                    created.setModelId(model.getId());
                    created.setCreatedBy(user.getUserId());
                    created.setCreatedAt(now);
                    created.setVersion(0);
                    return created;
                });
        assignment.setAssignmentRole(request.assignmentRole());
        assignment.setPriority(request.priority());
        assignment.setEnabled(true);
        assignment.setTemperatureOverride(request.temperatureOverride());
        assignment.setMaximumOutputTokensOverride(request.maximumOutputTokensOverride());
        assignment.setUpdatedBy(user.getUserId());
        assignment.setUpdatedAt(now);
        assignmentRepository.save(assignment);
        return toResponse(assignment, model, requireProvider(model.getProviderId(), user.getOrganizationId()));
    }

    @Transactional
    public AgentModelAssignmentResponse update(
            UUID projectId,
            UUID agentId,
            UUID assignmentId,
            UpdateAgentModelAssignmentRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_AGENT_ASSIGN);
        AgentModelAssignment assignment = requireAssignment(projectId, agentId, assignmentId, user.getOrganizationId());
        if (!assignment.getVersion().equals(request.version())) {
            throw conflict();
        }
        if (request.enabled() != null) {
            assignment.setEnabled(request.enabled());
        }
        if (request.priority() != null) {
            assignment.setPriority(request.priority());
        }
        if (request.temperatureOverride() != null) {
            assignment.setTemperatureOverride(request.temperatureOverride());
        }
        if (request.maximumOutputTokensOverride() != null) {
            assignment.setMaximumOutputTokensOverride(request.maximumOutputTokensOverride());
        }
        assignment.setUpdatedBy(user.getUserId());
        assignment.setUpdatedAt(Instant.now());
        assignmentRepository.save(assignment);
        AiModel model = requireActiveModel(assignment.getModelId(), user.getOrganizationId());
        return toResponse(assignment, model, requireProvider(model.getProviderId(), user.getOrganizationId()));
    }

    @Transactional
    public void remove(UUID projectId, UUID agentId, UUID assignmentId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_AGENT_ASSIGN);
        AgentModelAssignment assignment = requireAssignment(projectId, agentId, assignmentId, user.getOrganizationId());
        assignmentRepository.delete(assignment);
    }

    private void requireProjectModelEnabled(UUID projectId, UUID modelId, UUID organizationId) {
        ProjectModel projectModel = projectModelRepository
                .findByProjectIdAndModelIdAndOrganizationId(projectId, modelId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "MODEL_NOT_PROJECT_ENABLED", "Model not enabled for project"));
        if (!projectModel.isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_NOT_PROJECT_ENABLED", "Model not enabled for project");
        }
    }

    private List<AgentModelAssignmentResponse> toResponses(List<AgentModelAssignment> assignments, UUID organizationId) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<UUID> modelIds = assignments.stream().map(AgentModelAssignment::getModelId).distinct().toList();
        Map<UUID, AiModel> models = modelRepository.findByIdInAndOrganizationId(modelIds, organizationId).stream()
                .collect(Collectors.toMap(AiModel::getId, Function.identity()));
        List<AgentModelAssignmentResponse> responses = new ArrayList<>();
        for (AgentModelAssignment assignment : assignments) {
            AiModel model = models.get(assignment.getModelId());
            if (model == null) {
                continue;
            }
            AiProvider provider = requireProvider(model.getProviderId(), organizationId);
            responses.add(toResponse(assignment, model, provider));
        }
        return responses;
    }

    private AgentModelAssignmentResponse toResponse(
            AgentModelAssignment assignment, AiModel model, AiProvider provider) {
        return mapper.toAssignmentResponse(assignment, model, provider);
    }

    private AgentModelAssignment requireAssignment(
            UUID projectId, UUID agentId, UUID assignmentId, UUID organizationId) {
        return assignmentRepository
                .findByIdAndAgentIdAndProjectIdAndOrganizationId(assignmentId, agentId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASSIGNMENT_NOT_FOUND", "Not found"));
    }

    private AiModel requireActiveModel(UUID modelId, UUID organizationId) {
        AiModel model = modelRepository.findByIdInAndOrganizationId(List.of(modelId), organizationId).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Model not found"));
        if (model.getStatus() != AiModelStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_NOT_ACTIVE", "Model must be active");
        }
        return model;
    }

    private AiProvider requireProvider(UUID providerId, UUID organizationId) {
        return providerRepository
                .findByIdAndOrganizationId(providerId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND", "Provider not found"));
    }

    private void requireContext(UUID projectId, UUID agentId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
        agentRepository
                .findByIdAndProjectIdAndOrganizationId(agentId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
    }

    private static ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Resource was updated by another request");
    }
}
