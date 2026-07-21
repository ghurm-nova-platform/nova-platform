package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityRefreshTokenEntity;

public interface IdentityRefreshTokenRepository extends JpaRepository<IdentityRefreshTokenEntity, UUID> {

    java.util.Optional<IdentityRefreshTokenEntity> findByTokenHash(String tokenHash);

    List<IdentityRefreshTokenEntity> findByIdentityUserId(UUID identityUserId);
}
