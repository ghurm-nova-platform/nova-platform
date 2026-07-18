package ai.nova.platform.modelgateway.service;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.CreateRoutingPolicyRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.RoutingPolicyResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateRoutingPolicyRequest;
import ai.nova.platform.modelgateway.entity.ModelRoutingPolicy;
import ai.nova.platform.modelgateway.entity.RoutingPolicyStatus;
import ai.nova.platform.modelgateway.mapper.ModelGatewayMapper;
import ai.nova.platform.modelgateway.repository.ModelRoutingPolicyRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelRoutingPolicyService {

    private static final Pattern POLICY_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final ModelRoutingPolicyRepository policyRepository;
    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final ModelGatewayProperties properties;
    private final ModelGatewayMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;

    public ModelRoutingPolicyService(
            ModelRoutingPolicyRepository policyRepository,
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            ModelGatewayProperties properties,
            ModelGatewayMapper mapper,
            ModelGatewayAuthorizationService authorizationService) {
        this.policyRepository = policyRepository;
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.properties = properties;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<RoutingPolicyResponse> list(
            UUID projectId,
            UUID agentId,
            RoutingPolicyStatus status,
            Pageable pageable,
            AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ROUTE_READ);
        requireProject(projectId, user.getOrganizationId());
        return policyRepository
                .search(user.getOrganizationId(), projectId, agentId, status, pageable)
                .map(mapper::toRoutingPolicyResponse);
    }

    @Transactional(readOnly = true)
    public RoutingPolicyResponse get(UUID projectId, UUID policyId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ROUTE_READ);
        return mapper.toRoutingPolicyResponse(requirePolicy(projectId, policyId, user.getOrganizationId()));
    }

    @Transactional
    public RoutingPolicyResponse create(UUID projectId, CreateRoutingPolicyRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ROUTE_MANAGE);
        requireProject(projectId, user.getOrganizationId());
        if (request.agentId() != null) {
            requireAgent(projectId, request.agentId(), user.getOrganizationId());
        }
        String key = request.policyKey().trim().toUpperCase();
        if (!POLICY_KEY_PATTERN.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "POLICY_KEY_INVALID", "Invalid policy key");
        }
        if (policyRepository.existsByProjectIdAndAgentIdAndPolicyKey(projectId, request.agentId(), key)) {
            throw new ApiException(HttpStatus.CONFLICT, "POLICY_KEY_EXISTS", "Policy key already exists");
        }

        Instant now = Instant.now();
        ModelRoutingPolicy policy = new ModelRoutingPolicy();
        policy.setId(UUID.randomUUID());
        policy.setOrganizationId(user.getOrganizationId());
        policy.setProjectId(projectId);
        policy.setAgentId(request.agentId());
        policy.setPolicyKey(key);
        policy.setName(request.name().trim());
        policy.setDescription(trim(request.description()));
        policy.setStatus(RoutingPolicyStatus.DRAFT);
        policy.setStrategy(request.strategy());
        policy.setFallbackEnabled(request.fallbackEnabled() == null || request.fallbackEnabled());
        policy.setRetryEnabled(request.retryEnabled() == null || request.retryEnabled());
        policy.setMaximumProviderAttempts(
                request.maximumProviderAttempts() != null
                        ? request.maximumProviderAttempts()
                        : properties.getMaxProviderAttempts());
        policy.setMaximumTotalDurationMs(
                request.maximumTotalDurationMs() != null
                        ? request.maximumTotalDurationMs()
                        : properties.getMaxTotalDurationMs());
        policy.setRequireToolSupport(Boolean.TRUE.equals(request.requireToolSupport()));
        policy.setRequireKnowledgeSupport(Boolean.TRUE.equals(request.requireKnowledgeSupport()));
        policy.setCreatedBy(user.getUserId());
        policy.setUpdatedBy(user.getUserId());
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        policy.setVersion(0);
        return mapper.toRoutingPolicyResponse(policyRepository.save(policy));
    }

    @Transactional
    public RoutingPolicyResponse update(
            UUID projectId, UUID policyId, UpdateRoutingPolicyRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ROUTE_MANAGE);
        ModelRoutingPolicy policy = requirePolicy(projectId, policyId, user.getOrganizationId());
        if (!policy.getVersion().equals(request.version())) {
            throw conflict();
        }
        policy.setName(request.name().trim());
        policy.setDescription(trim(request.description()));
        policy.setStrategy(request.strategy());
        if (request.fallbackEnabled() != null) {
            policy.setFallbackEnabled(request.fallbackEnabled());
        }
        if (request.retryEnabled() != null) {
            policy.setRetryEnabled(request.retryEnabled());
        }
        if (request.maximumProviderAttempts() != null) {
            policy.setMaximumProviderAttempts(request.maximumProviderAttempts());
        }
        if (request.maximumTotalDurationMs() != null) {
            policy.setMaximumTotalDurationMs(request.maximumTotalDurationMs());
        }
        if (request.requireToolSupport() != null) {
            policy.setRequireToolSupport(request.requireToolSupport());
        }
        if (request.requireKnowledgeSupport() != null) {
            policy.setRequireKnowledgeSupport(request.requireKnowledgeSupport());
        }
        policy.setUpdatedBy(user.getUserId());
        policy.setUpdatedAt(Instant.now());
        return mapper.toRoutingPolicyResponse(policyRepository.save(policy));
    }

    @Transactional
    public RoutingPolicyResponse activate(UUID projectId, UUID policyId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ROUTE_MANAGE);
        ModelRoutingPolicy policy = requirePolicy(projectId, policyId, user.getOrganizationId());
        policy.setStatus(RoutingPolicyStatus.ACTIVE);
        policy.setUpdatedBy(user.getUserId());
        policy.setUpdatedAt(Instant.now());
        return mapper.toRoutingPolicyResponse(policyRepository.save(policy));
    }

    @Transactional
    public RoutingPolicyResponse archive(UUID projectId, UUID policyId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ROUTE_MANAGE);
        ModelRoutingPolicy policy = requirePolicy(projectId, policyId, user.getOrganizationId());
        policy.setStatus(RoutingPolicyStatus.ARCHIVED);
        policy.setUpdatedBy(user.getUserId());
        policy.setUpdatedAt(Instant.now());
        return mapper.toRoutingPolicyResponse(policyRepository.save(policy));
    }

    private ModelRoutingPolicy requirePolicy(UUID projectId, UUID policyId, UUID organizationId) {
        return policyRepository
                .findByIdAndProjectIdAndOrganizationId(policyId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POLICY_NOT_FOUND", "Policy not found"));
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void requireAgent(UUID projectId, UUID agentId, UUID organizationId) {
        agentRepository
                .findByIdAndProjectIdAndOrganizationId(agentId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Resource was updated by another request");
    }
}
