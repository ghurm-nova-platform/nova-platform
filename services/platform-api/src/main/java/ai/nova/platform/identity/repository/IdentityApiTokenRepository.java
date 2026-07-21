package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityApiTokenEntity;

public interface IdentityApiTokenRepository extends JpaRepository<IdentityApiTokenEntity, UUID> {

    java.util.Optional<IdentityApiTokenEntity> findByTokenHash(String tokenHash);

    List<IdentityApiTokenEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
