package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.ProviderStatus;
import ai.nova.platform.identity.entity.ProviderType;

public interface IdentityProviderRepository extends JpaRepository<IdentityProviderEntity, UUID> {

    List<IdentityProviderEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<IdentityProviderEntity> findByOrganizationIdAndProviderTypeAndStatus(
            UUID organizationId, ProviderType providerType, ProviderStatus status);

    Optional<IdentityProviderEntity> findByOrganizationIdAndDefaultProviderTrue(UUID organizationId);
}
