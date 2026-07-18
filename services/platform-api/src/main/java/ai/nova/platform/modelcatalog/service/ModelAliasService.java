package ai.nova.platform.modelcatalog.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.AliasResponse;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CreateAliasRequest;
import ai.nova.platform.modelcatalog.entity.AiModelAlias;
import ai.nova.platform.modelcatalog.mapper.ModelCatalogMapper;
import ai.nova.platform.modelcatalog.repository.AiModelAliasRepository;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.security.ModelGatewayAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelAliasService {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._:-]{0,149}$", Pattern.CASE_INSENSITIVE);

    private final AiModelAliasRepository aliasRepository;
    private final AiModelRepository modelRepository;
    private final ModelCatalogMapper mapper;
    private final ModelGatewayAuthorizationService authorizationService;

    public ModelAliasService(
            AiModelAliasRepository aliasRepository,
            AiModelRepository modelRepository,
            ModelCatalogMapper mapper,
            ModelGatewayAuthorizationService authorizationService) {
        this.aliasRepository = aliasRepository;
        this.modelRepository = modelRepository;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<AliasResponse> list(UUID modelId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_CATALOG_READ);
        requireModel(modelId, user.getOrganizationId());
        return aliasRepository
                .findByModelIdAndOrganizationIdOrderByCreatedAtAsc(modelId, user.getOrganizationId())
                .stream()
                .map(mapper::toAliasResponse)
                .toList();
    }

    @Transactional
    public AliasResponse create(UUID modelId, CreateAliasRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ALIAS_MANAGE);
        AiModel model = requireModel(modelId, user.getOrganizationId());
        if (model.getStatus() == AiModelStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "MODEL_ARCHIVED", "Cannot alias an archived model");
        }
        String alias = request.alias().trim();
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALIAS_INVALID", "Invalid alias format");
        }
        String normalized = normalize(alias);
        if (modelRepository.existsByOrganizationIdAndModelKey(user.getOrganizationId(), normalized)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ALIAS_COLLIDES_WITH_KEY", "Alias collides with an existing model key");
        }
        if (aliasRepository.existsByOrganizationIdAndNormalizedAlias(user.getOrganizationId(), normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALIAS_EXISTS", "Alias already exists");
        }

        AiModelAlias entity = new AiModelAlias();
        entity.setId(UUID.randomUUID());
        entity.setOrganizationId(user.getOrganizationId());
        entity.setModelId(modelId);
        entity.setAlias(alias);
        entity.setNormalizedAlias(normalized);
        entity.setCreatedBy(user.getUserId());
        entity.setCreatedAt(Instant.now());
        return mapper.toAliasResponse(aliasRepository.save(entity));
    }

    @Transactional
    public void delete(UUID aliasId, AuthenticatedUser user) {
        authorizationService.require(user, ModelGatewayAuthorizationService.MODEL_ALIAS_MANAGE);
        AiModelAlias alias = aliasRepository
                .findByIdAndOrganizationId(aliasId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ALIAS_NOT_FOUND", "Alias not found"));
        aliasRepository.delete(alias);
    }

    public static String normalize(String alias) {
        return alias.trim().toLowerCase(Locale.ROOT);
    }

    private AiModel requireModel(UUID modelId, UUID organizationId) {
        return modelRepository
                .findByIdAndOrganizationId(modelId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "Model not found"));
    }
}
