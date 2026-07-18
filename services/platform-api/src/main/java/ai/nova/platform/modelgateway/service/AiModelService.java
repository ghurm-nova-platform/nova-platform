package ai.nova.platform.modelgateway.service;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.CreateModelRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ModelResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateModelRequest;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.mapper.ModelGatewayMapper;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AiModelService {

    private static final Pattern MODEL_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final AiModelRepository modelRepository;
    private final AiProviderService providerService;
    private final ModelGatewayMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;

    public AiModelService(
            AiModelRepository modelRepository,
            AiProviderService providerService,
            ModelGatewayMapper mapper,
            ModelGatewayAuthorizationService authorizationService) {
        this.modelRepository = modelRepository;
        this.providerService = providerService;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<ModelResponse> list(
            UUID providerId, AiModelStatus status, String search, Pageable pageable, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_READ);
        providerService.requireProvider(providerId, user.getOrganizationId());
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return modelRepository
                .search(user.getOrganizationId(), providerId, status, normalizedSearch, pageable)
                .map(mapper::toModelResponse);
    }

    @Transactional(readOnly = true)
    public ModelResponse get(UUID providerId, UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_READ);
        return mapper.toModelResponse(requireModel(providerId, modelId, user.getOrganizationId()));
    }

    @Transactional
    public ModelResponse create(UUID providerId, CreateModelRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CREATE);
        AiProvider provider = providerService.requireProvider(providerId, user.getOrganizationId());
        String key = request.modelKey().trim().toUpperCase();
        if (!MODEL_KEY_PATTERN.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MODEL_KEY_INVALID", "Invalid model key");
        }
        if (modelRepository.existsByProviderIdAndModelKey(providerId, key)) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_KEY_EXISTS", "Model key already exists");
        }
        if (request.maxOutputTokens() > request.contextWindowTokens()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "maxOutputTokens exceeds context window");
        }

        Instant now = Instant.now();
        AiModel model = new AiModel();
        model.setId(UUID.randomUUID());
        model.setOrganizationId(user.getOrganizationId());
        model.setProviderId(providerId);
        model.setModelKey(key);
        model.setProviderModelId(request.providerModelId().trim());
        model.setDisplayName(request.displayName().trim());
        model.setDescription(trim(request.description()));
        model.setModelType(request.modelType());
        model.setStatus(AiModelStatus.DRAFT);
        model.setContextWindowTokens(request.contextWindowTokens());
        model.setMaxOutputTokens(request.maxOutputTokens());
        model.setSupportsTools(Boolean.TRUE.equals(request.supportsTools()));
        model.setSupportsKnowledgeContext(
                request.supportsKnowledgeContext() == null || request.supportsKnowledgeContext());
        model.setSupportsJsonOutput(Boolean.TRUE.equals(request.supportsJsonOutput()));
        model.setSupportsStreaming(Boolean.TRUE.equals(request.supportsStreaming()));
        model.setSupportsSystemMessages(
                request.supportsSystemMessages() == null || request.supportsSystemMessages());
        model.setInputCostPerMillion(request.inputCostPerMillion());
        model.setOutputCostPerMillion(request.outputCostPerMillion());
        model.setCurrencyCode(trim(request.currencyCode()));
        model.setCreatedBy(user.getUserId());
        model.setUpdatedBy(user.getUserId());
        model.setCreatedAt(now);
        model.setUpdatedAt(now);
        model.setVersion(0);
        return mapper.toModelResponse(modelRepository.save(model));
    }

    @Transactional
    public ModelResponse update(UUID providerId, UUID modelId, UpdateModelRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_UPDATE);
        AiModel model = requireModel(providerId, modelId, user.getOrganizationId());
        if (!model.getVersion().equals(request.version())) {
            throw conflict();
        }
        if (request.maxOutputTokens() > request.contextWindowTokens()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "maxOutputTokens exceeds context window");
        }
        model.setDisplayName(request.displayName().trim());
        model.setDescription(trim(request.description()));
        model.setContextWindowTokens(request.contextWindowTokens());
        model.setMaxOutputTokens(request.maxOutputTokens());
        if (request.supportsTools() != null) {
            model.setSupportsTools(request.supportsTools());
        }
        if (request.supportsKnowledgeContext() != null) {
            model.setSupportsKnowledgeContext(request.supportsKnowledgeContext());
        }
        if (request.supportsJsonOutput() != null) {
            model.setSupportsJsonOutput(request.supportsJsonOutput());
        }
        if (request.supportsStreaming() != null) {
            model.setSupportsStreaming(request.supportsStreaming());
        }
        if (request.supportsSystemMessages() != null) {
            model.setSupportsSystemMessages(request.supportsSystemMessages());
        }
        model.setInputCostPerMillion(request.inputCostPerMillion());
        model.setOutputCostPerMillion(request.outputCostPerMillion());
        model.setCurrencyCode(trim(request.currencyCode()));
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        return mapper.toModelResponse(modelRepository.save(model));
    }

    @Transactional
    public ModelResponse activate(UUID providerId, UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ACTIVATE);
        AiModel model = requireModel(providerId, modelId, user.getOrganizationId());
        AiProvider provider = providerService.requireProvider(providerId, user.getOrganizationId());
        if (provider.getStatus() != AiProviderStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "PROVIDER_NOT_ACTIVE", "Provider must be active");
        }
        model.setStatus(AiModelStatus.ACTIVE);
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        return mapper.toModelResponse(modelRepository.save(model));
    }

    @Transactional
    public ModelResponse disable(UUID providerId, UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_DISABLE);
        AiModel model = requireModel(providerId, modelId, user.getOrganizationId());
        model.setStatus(AiModelStatus.DISABLED);
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        return mapper.toModelResponse(modelRepository.save(model));
    }

    @Transactional
    public ModelResponse archive(UUID providerId, UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ARCHIVE);
        AiModel model = requireModel(providerId, modelId, user.getOrganizationId());
        model.setStatus(AiModelStatus.ARCHIVED);
        model.setUpdatedBy(user.getUserId());
        model.setUpdatedAt(Instant.now());
        return mapper.toModelResponse(modelRepository.save(model));
    }

    AiModel requireModel(UUID providerId, UUID modelId, UUID organizationId) {
        return modelRepository
                .findByIdAndProviderIdAndOrganizationId(modelId, providerId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Model not found"));
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Resource was updated by another request");
    }
}
