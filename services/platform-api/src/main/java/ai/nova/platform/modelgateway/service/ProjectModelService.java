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

import ai.nova.platform.modelgateway.audit.ModelGatewayAuditService;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.AssignProjectModelRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProjectModelResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateProjectModelRequest;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.ProjectModel;
import ai.nova.platform.modelgateway.mapper.ModelGatewayMapper;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.repository.ProjectModelRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ProjectModelService {

    private final ProjectModelRepository projectModelRepository;
    private final AiModelRepository modelRepository;
    private final AiProviderRepository providerRepository;
    private final ProjectRepository projectRepository;
    private final ModelGatewayMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;
    private final ModelGatewayAuditService auditService;

    public ProjectModelService(
            ProjectModelRepository projectModelRepository,
            AiModelRepository modelRepository,
            AiProviderRepository providerRepository,
            ProjectRepository projectRepository,
            ModelGatewayMapper mapper,
            ModelGatewayAuthorizationService authorizationService,
            ModelGatewayAuditService auditService) {
        this.projectModelRepository = projectModelRepository;
        this.modelRepository = modelRepository;
        this.providerRepository = providerRepository;
        this.projectRepository = projectRepository;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProjectModelResponse> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_READ);
        requireProject(projectId, user.getOrganizationId());
        List<ProjectModel> assignments =
                projectModelRepository.findByProjectIdAndOrganizationIdOrderByCreatedAtAsc(
                        projectId, user.getOrganizationId());
        return toResponses(assignments, user.getOrganizationId());
    }

    @Transactional
    public ProjectModelResponse assign(UUID projectId, AssignProjectModelRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROJECT_ASSIGN);
        requireProject(projectId, user.getOrganizationId());
        AiModel model = modelRepository.findByIdInAndOrganizationId(List.of(request.modelId()), user.getOrganizationId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Model not found"));
        if (model.getStatus() != AiModelStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_NOT_ACTIVE", "Model must be active");
        }

        Instant now = Instant.now();
        ProjectModel projectModel = projectModelRepository
                .findByProjectIdAndModelIdAndOrganizationId(projectId, model.getId(), user.getOrganizationId())
                .orElseGet(() -> {
                    ProjectModel created = new ProjectModel();
                    created.setId(UUID.randomUUID());
                    created.setOrganizationId(user.getOrganizationId());
                    created.setProjectId(projectId);
                    created.setModelId(model.getId());
                    created.setEnabled(true);
                    created.setDefault(false);
                    created.setCreatedBy(user.getUserId());
                    created.setUpdatedBy(user.getUserId());
                    created.setCreatedAt(now);
                    created.setUpdatedAt(now);
                    created.setVersion(0);
                    return created;
                });
        projectModel.setEnabled(true);
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearOtherDefaults(projectId, user.getOrganizationId(), projectModel.getId());
            projectModel.setDefault(true);
        }
        projectModel.setUpdatedBy(user.getUserId());
        projectModel.setUpdatedAt(now);
        projectModelRepository.save(projectModel);
        auditService.modelAssignedToProject(
                user.getOrganizationId(), projectId, model.getId(), user.getUserId());
        return toResponse(projectModel, model, requireProvider(model.getProviderId(), user.getOrganizationId()));
    }

    @Transactional
    public ProjectModelResponse update(
            UUID projectId, UUID id, UpdateProjectModelRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_PROJECT_ASSIGN);
        ProjectModel projectModel = requireProjectModel(projectId, id, user.getOrganizationId());
        if (!projectModel.getVersion().equals(request.version())) {
            throw conflict();
        }
        if (request.enabled() != null) {
            projectModel.setEnabled(request.enabled());
        }
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearOtherDefaults(projectId, user.getOrganizationId(), projectModel.getId());
            projectModel.setDefault(true);
        } else if (Boolean.FALSE.equals(request.isDefault())) {
            projectModel.setDefault(false);
        }
        if (request.maximumInputTokensOverride() != null) {
            projectModel.setMaximumInputTokensOverride(request.maximumInputTokensOverride());
        }
        if (request.maximumOutputTokensOverride() != null) {
            projectModel.setMaximumOutputTokensOverride(request.maximumOutputTokensOverride());
        }
        if (request.dailyRequestLimit() != null) {
            projectModel.setDailyRequestLimit(request.dailyRequestLimit());
        }
        if (request.monthlyRequestLimit() != null) {
            projectModel.setMonthlyRequestLimit(request.monthlyRequestLimit());
        }
        projectModel.setUpdatedBy(user.getUserId());
        projectModel.setUpdatedAt(Instant.now());
        projectModelRepository.save(projectModel);
        AiModel model = requireModel(projectModel.getModelId(), user.getOrganizationId());
        return toResponse(projectModel, model, requireProvider(model.getProviderId(), user.getOrganizationId()));
    }

    private void clearOtherDefaults(UUID projectId, UUID organizationId, UUID keepId) {
        List<ProjectModel> defaults = projectModelRepository.lockEnabledDefaults(projectId, organizationId);
        for (ProjectModel existing : defaults) {
            if (!existing.getId().equals(keepId)) {
                existing.setDefault(false);
                existing.setUpdatedAt(Instant.now());
                projectModelRepository.save(existing);
            }
        }
    }

    private List<ProjectModelResponse> toResponses(List<ProjectModel> assignments, UUID organizationId) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<UUID> modelIds = assignments.stream().map(ProjectModel::getModelId).distinct().toList();
        Map<UUID, AiModel> models = modelRepository.findByIdInAndOrganizationId(modelIds, organizationId).stream()
                .collect(Collectors.toMap(AiModel::getId, Function.identity()));
        List<ProjectModelResponse> responses = new ArrayList<>();
        for (ProjectModel assignment : assignments) {
            AiModel model = models.get(assignment.getModelId());
            if (model == null) {
                continue;
            }
            AiProvider provider = requireProvider(model.getProviderId(), organizationId);
            responses.add(toResponse(assignment, model, provider));
        }
        return responses;
    }

    private ProjectModelResponse toResponse(ProjectModel projectModel, AiModel model, AiProvider provider) {
        return mapper.toProjectModelResponse(projectModel, model, provider);
    }

    private ProjectModel requireProjectModel(UUID projectId, UUID id, UUID organizationId) {
        return projectModelRepository
                .findByIdAndProjectIdAndOrganizationId(id, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_MODEL_NOT_FOUND", "Not found"));
    }

    private AiModel requireModel(UUID modelId, UUID organizationId) {
        return modelRepository.findByIdInAndOrganizationId(List.of(modelId), organizationId).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Model not found"));
    }

    private AiProvider requireProvider(UUID providerId, UUID organizationId) {
        return providerRepository
                .findByIdAndOrganizationId(providerId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND", "Provider not found"));
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private static ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Resource was updated by another request");
    }
}
