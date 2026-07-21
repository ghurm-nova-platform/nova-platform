package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.CreateProviderRequest;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderView;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.ProviderStatus;
import ai.nova.platform.identity.entity.ProviderType;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityProviderRepository;
import ai.nova.platform.web.error.ResourceNotFoundException;

@Service
public class IdentityProviderService {

    private final IdentityProviderRepository providerRepository;

    public IdentityProviderService(IdentityProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderView> listProviders(UUID organizationId) {
        return providerRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(IdentityEntityMapper::toProviderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public IdentityProviderEntity getProvider(UUID providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
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
