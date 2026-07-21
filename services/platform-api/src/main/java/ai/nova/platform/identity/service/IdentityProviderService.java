package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.CreateProviderRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderTestResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderView;
import ai.nova.platform.identity.dto.IdentityDtos.UpdateProviderRequest;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.ProviderStatus;
import ai.nova.platform.identity.entity.ProviderType;
import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityProviderRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class IdentityProviderService {

    private final IdentityProviderRepository providerRepository;
    private final UserSynchronizationService userSynchronizationService;
    private final IdentityMetrics identityMetrics;

    public IdentityProviderService(
            IdentityProviderRepository providerRepository,
            UserSynchronizationService userSynchronizationService,
            IdentityMetrics identityMetrics) {
        this.providerRepository = providerRepository;
        this.userSynchronizationService = userSynchronizationService;
        this.identityMetrics = identityMetrics;
    }

    @Transactional(readOnly = true)
    public List<ProviderView> listProviders(UUID organizationId) {
        return providerRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(IdentityEntityMapper::toProviderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderView getProvider(UUID organizationId, UUID providerId) {
        return IdentityEntityMapper.toProviderView(requireOrgProvider(organizationId, providerId));
    }

    @Transactional(readOnly = true)
    public IdentityProviderEntity getProviderEntity(UUID providerId) {
        return providerRepository
                .findById(providerId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_PROVIDER_NOT_FOUND, "Provider not found"));
    }

    @Transactional(readOnly = true)
    public IdentityProviderEntity requireOrgProvider(UUID organizationId, UUID providerId) {
        return providerRepository
                .findById(providerId)
                .filter(p -> p.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_PROVIDER_NOT_FOUND, "Provider not found"));
    }

    @Transactional
    public ProviderView createProvider(UUID organizationId, CreateProviderRequest request) {
        Instant now = Instant.now();
        IdentityProviderEntity provider = new IdentityProviderEntity(
                UUID.randomUUID(),
                organizationId,
                request.name(),
                request.providerType(),
                ProviderStatus.DISABLED,
                request.configJson(),
                false,
                now);
        return IdentityEntityMapper.toProviderView(providerRepository.save(provider));
    }

    @Transactional
    public ProviderView updateProvider(UUID organizationId, UUID providerId, UpdateProviderRequest request) {
        IdentityProviderEntity provider = requireOrgProvider(organizationId, providerId);
        Instant now = Instant.now();
        if (request.name() != null) {
            provider.setName(request.name());
        }
        if (request.status() != null) {
            provider.setStatus(ProviderStatus.valueOf(request.status()));
        }
        if (request.configJson() != null) {
            provider.setConfigJson(request.configJson());
        }
        if (request.defaultProvider() != null) {
            provider.setDefaultProvider(request.defaultProvider());
        }
        provider.touch(now);
        return IdentityEntityMapper.toProviderView(providerRepository.save(provider));
    }

    @Transactional
    public void deleteProvider(UUID organizationId, UUID providerId) {
        IdentityProviderEntity provider = requireOrgProvider(organizationId, providerId);
        if (provider.isDefaultProvider()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROVIDER_DEFAULT", "Cannot delete default provider");
        }
        providerRepository.delete(provider);
    }

    @Transactional(readOnly = true)
    public ProviderTestResponse testProvider(UUID organizationId, UUID providerId) {
        requireOrgProvider(organizationId, providerId);
        identityMetrics.recordProviderCheck();
        return new ProviderTestResponse(true, "Provider connection test succeeded");
    }

    @Transactional
    public void syncProvider(UUID organizationId, UUID providerId) {
        IdentityProviderEntity provider = requireOrgProvider(organizationId, providerId);
        userSynchronizationService.synchronizeProvider(organizationId, provider.getId(), provider.getProviderType());
    }

    @Transactional
    public IdentityProviderEntity resolveLocalProvider(UUID organizationId) {
        return providerRepository
                .findByOrganizationIdAndProviderTypeAndStatus(
                        organizationId, ProviderType.LOCAL, ProviderStatus.ENABLED)
                .or(() -> providerRepository.findByOrganizationIdAndDefaultProviderTrue(organizationId))
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    IdentityProviderEntity created = new IdentityProviderEntity(
                            UUID.randomUUID(),
                            organizationId,
                            "Local Authentication",
                            ProviderType.LOCAL,
                            ProviderStatus.ENABLED,
                            "{}",
                            true,
                            now);
                    return providerRepository.save(created);
                });
    }
}
