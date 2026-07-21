package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityUserEntity;

public interface IdentityUserRepository extends JpaRepository<IdentityUserEntity, UUID> {

    Optional<IdentityUserEntity> findByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    Optional<IdentityUserEntity> findByEmailIgnoreCase(String email);

    Optional<IdentityUserEntity> findByPlatformUserId(UUID platformUserId);

    List<IdentityUserEntity> findByOrganizationIdOrderByEmailAsc(UUID organizationId);
}
