package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityRecoveryCodeEntity;

public interface IdentityRecoveryCodeRepository extends JpaRepository<IdentityRecoveryCodeEntity, UUID> {

    List<IdentityRecoveryCodeEntity> findByIdentityUserIdAndUsedAtIsNull(UUID identityUserId);
}
