package ai.nova.platform.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityApiTokenEntity;

public interface IdentityApiTokenRepository extends JpaRepository<IdentityApiTokenEntity, UUID> {

    Optional<IdentityApiTokenEntity> findByTokenHash(String tokenHash);
}
