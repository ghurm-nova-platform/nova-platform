package ai.nova.platform.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityServiceAccountEntity;

public interface IdentityServiceAccountRepository extends JpaRepository<IdentityServiceAccountEntity, UUID> {

    Optional<IdentityServiceAccountEntity> findByClientId(String clientId);
}
