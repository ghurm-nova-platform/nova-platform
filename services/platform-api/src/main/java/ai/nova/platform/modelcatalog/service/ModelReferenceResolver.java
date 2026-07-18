package ai.nova.platform.modelcatalog.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.modelcatalog.entity.AiModelAlias;
import ai.nova.platform.modelcatalog.repository.AiModelAliasRepository;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.AiProviderRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class ModelReferenceResolver {

    private final AiModelRepository modelRepository;
    private final AiModelAliasRepository aliasRepository;
    private final AiProviderRepository providerRepository;

    public ModelReferenceResolver(
            AiModelRepository modelRepository,
            AiModelAliasRepository aliasRepository,
            AiProviderRepository providerRepository) {
        this.modelRepository = modelRepository;
        this.aliasRepository = aliasRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public ResolvedModel resolve(UUID organizationId, String modelReference) {
        if (modelReference == null || modelReference.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MODEL_REFERENCE_INVALID", "Model reference is required");
        }
        String raw = modelReference.trim();
        String normalized = raw.toLowerCase(Locale.ROOT);

        Optional<AiModel> byKey = modelRepository.findByOrganizationIdAndModelKeyAndStatus(
                organizationId, normalized, AiModelStatus.ACTIVE);
        Optional<AiModel> byAlias = aliasRepository
                .findByOrganizationIdAndNormalizedAlias(organizationId, normalized)
                .flatMap(alias -> modelRepository.findByIdAndOrganizationId(alias.getModelId(), organizationId))
                .filter(model -> model.getStatus() == AiModelStatus.ACTIVE);

        if (byKey.isPresent() && byAlias.isPresent() && !byKey.get().getId().equals(byAlias.get().getId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "MODEL_REFERENCE_AMBIGUOUS", "Model reference matches key and alias");
        }

        AiModel model = byKey.or(() -> byAlias).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "MODEL_REFERENCE_NOT_FOUND", "Model reference not found"));

        AiProvider provider = providerRepository
                .findByIdAndOrganizationId(model.getProviderId(), organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND", "Provider not found"));
        if (provider.getStatus() != AiProviderStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "PROVIDER_NOT_ACTIVE", "Provider must be active");
        }
        return new ResolvedModel(model, provider, model.getModelKey());
    }

    public record ResolvedModel(AiModel model, AiProvider provider, String canonicalKey) {
    }
}
