package ai.nova.platform.modelgateway.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelgateway.entity.AgentModelAssignment;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiModelType;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.entity.AssignmentRole;
import ai.nova.platform.modelgateway.entity.ModelRoutingPolicy;
import ai.nova.platform.modelgateway.entity.ProjectModel;
import ai.nova.platform.modelgateway.entity.RoutingPolicyStatus;
import ai.nova.platform.modelgateway.entity.RoutingStrategy;
import ai.nova.platform.modelgateway.gateway.ModelGatewayRequest;
import ai.nova.platform.modelgateway.provider.AiModelProviderRegistry;
import ai.nova.platform.modelgateway.repository.AgentModelAssignmentRepository;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.modelgateway.repository.ModelRoutingPolicyRepository;
import ai.nova.platform.modelgateway.repository.ProjectModelRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelRoutingService {

    private static final Comparator<AgentModelAssignment> ASSIGNMENT_ORDER = Comparator
            .comparing(AgentModelAssignment::getAssignmentRole)
            .thenComparing(AgentModelAssignment::getPriority)
            .thenComparing(AgentModelAssignment::getModelId);

    private final AgentModelAssignmentRepository assignmentRepository;
    private final AiModelRepository modelRepository;
    private final AiProviderRepository providerRepository;
    private final ProjectModelRepository projectModelRepository;
    private final ModelRoutingPolicyRepository policyRepository;
    private final AiModelProviderRegistry providerRegistry;

    public ModelRoutingService(
            AgentModelAssignmentRepository assignmentRepository,
            AiModelRepository modelRepository,
            AiProviderRepository providerRepository,
            ProjectModelRepository projectModelRepository,
            ModelRoutingPolicyRepository policyRepository,
            AiModelProviderRegistry providerRegistry) {
        this.assignmentRepository = assignmentRepository;
        this.modelRepository = modelRepository;
        this.providerRepository = providerRepository;
        this.projectModelRepository = projectModelRepository;
        this.policyRepository = policyRepository;
        this.providerRegistry = providerRegistry;
    }

    @Transactional(readOnly = true)
    public ResolvedRouting resolve(ModelGatewayRequest request) {
        ModelRoutingPolicy policy = resolvePolicy(request.projectId(), request.agentId(), request.organizationId());
        List<AgentModelAssignment> assignments = assignmentRepository
                .findByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrueOrderByAssignmentRoleAscPriorityAscModelIdAsc(
                        request.agentId(), request.projectId(), request.organizationId());
        if (assignments.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_NOT_ASSIGNED", "No enabled model assignments");
        }

        Map<UUID, AiModel> modelsById = loadModels(assignments, request.organizationId());
        Map<UUID, AiProvider> providersById = loadProviders(modelsById.values(), request.organizationId());
        Map<UUID, ProjectModel> projectModelsByModelId =
                loadProjectModels(request.projectId(), request.organizationId(), modelsById.keySet());

        boolean requireTools = policy != null && policy.isRequireToolSupport() || request.requiresTools();
        boolean requireKnowledge =
                policy != null && policy.isRequireKnowledgeSupport() || request.requiresKnowledge();

        List<RoutedModelCandidate> candidates = buildCandidates(
                assignments,
                modelsById,
                providersById,
                projectModelsByModelId,
                requireTools,
                requireKnowledge);

        RoutingStrategy strategy = policy != null ? policy.getStrategy() : RoutingStrategy.PRIORITY_FALLBACK;
        boolean fallbackEnabled = policy == null || policy.isFallbackEnabled();

        List<RoutedModelCandidate> ordered = orderCandidates(candidates, strategy, fallbackEnabled);
        if (ordered.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_NOT_AVAILABLE", "No eligible models for routing");
        }

        return new ResolvedRouting(policy, ordered);
    }

    private ModelRoutingPolicy resolvePolicy(UUID projectId, UUID agentId, UUID organizationId) {
        return policyRepository
                .findFirstByProjectIdAndAgentIdAndOrganizationIdAndStatusOrderByUpdatedAtDesc(
                        projectId, agentId, organizationId, RoutingPolicyStatus.ACTIVE)
                .or(() -> policyRepository.findFirstByProjectIdAndAgentIdIsNullAndOrganizationIdAndStatusOrderByUpdatedAtDesc(
                        projectId, organizationId, RoutingPolicyStatus.ACTIVE))
                .orElse(null);
    }

    private Map<UUID, AiModel> loadModels(List<AgentModelAssignment> assignments, UUID organizationId) {
        List<UUID> modelIds = assignments.stream().map(AgentModelAssignment::getModelId).distinct().toList();
        return modelRepository.findByIdInAndOrganizationId(modelIds, organizationId).stream()
                .collect(Collectors.toMap(AiModel::getId, Function.identity()));
    }

    private Map<UUID, AiProvider> loadProviders(Iterable<AiModel> models, UUID organizationId) {
        List<UUID> providerIds = new ArrayList<>();
        for (AiModel model : models) {
            providerIds.add(model.getProviderId());
        }
        return providerIds.stream()
                .distinct()
                .map(id -> providerRepository.findByIdAndOrganizationId(id, organizationId))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(AiProvider::getId, Function.identity()));
    }

    private Map<UUID, ProjectModel> loadProjectModels(
            UUID projectId, UUID organizationId, Iterable<UUID> modelIds) {
        return projectModelRepository.findByProjectIdAndOrganizationIdOrderByCreatedAtAsc(projectId, organizationId)
                .stream()
                .filter(pm -> {
                    for (UUID modelId : modelIds) {
                        if (modelId.equals(pm.getModelId())) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toMap(ProjectModel::getModelId, Function.identity(), (a, b) -> a));
    }

    private List<RoutedModelCandidate> buildCandidates(
            List<AgentModelAssignment> assignments,
            Map<UUID, AiModel> modelsById,
            Map<UUID, AiProvider> providersById,
            Map<UUID, ProjectModel> projectModelsByModelId,
            boolean requireTools,
            boolean requireKnowledge) {
        List<RoutedModelCandidate> candidates = new ArrayList<>();
        for (AgentModelAssignment assignment : assignments) {
            AiModel model = modelsById.get(assignment.getModelId());
            if (model == null || model.getStatus() != AiModelStatus.ACTIVE) {
                continue;
            }
            if (model.getModelType() != AiModelType.CHAT && model.getModelType() != AiModelType.TEXT_GENERATION) {
                continue;
            }
            AiProvider provider = providersById.get(model.getProviderId());
            if (provider == null || provider.getStatus() != AiProviderStatus.ACTIVE) {
                continue;
            }
            if (!providerRegistry.isRegistered(provider.getAdapterKey())) {
                continue;
            }
            ProjectModel projectModel = projectModelsByModelId.get(model.getId());
            if (projectModel == null || !projectModel.isEnabled()) {
                continue;
            }
            if (requireTools && !model.isSupportsTools()) {
                continue;
            }
            if (requireKnowledge && !model.isSupportsKnowledgeContext()) {
                continue;
            }
            candidates.add(new RoutedModelCandidate(
                    assignment,
                    model,
                    provider,
                    projectModel,
                    assignment.getAssignmentRole() == AssignmentRole.FALLBACK));
        }
        return candidates;
    }

    private List<RoutedModelCandidate> orderCandidates(
            List<RoutedModelCandidate> candidates, RoutingStrategy strategy, boolean fallbackEnabled) {
        List<RoutedModelCandidate> primary = candidates.stream()
                .filter(candidate -> !candidate.fallback())
                .sorted(Comparator.comparing(c -> c.assignment(), ASSIGNMENT_ORDER))
                .toList();
        List<RoutedModelCandidate> fallback = candidates.stream()
                .filter(RoutedModelCandidate::fallback)
                .sorted(Comparator.comparing(c -> c.assignment(), ASSIGNMENT_ORDER))
                .toList();

        return switch (strategy) {
            case FIXED_PRIMARY -> primary.isEmpty() && fallbackEnabled ? fallback : primary.stream().limit(1).toList();
            case PRIORITY_FALLBACK, CAPABILITY_BASED -> {
                List<RoutedModelCandidate> ordered = new ArrayList<>(primary);
                if (fallbackEnabled) {
                    ordered.addAll(fallback);
                }
                yield ordered;
            }
        };
    }

    public record ResolvedRouting(ModelRoutingPolicy policy, List<RoutedModelCandidate> candidates) {
    }
}
