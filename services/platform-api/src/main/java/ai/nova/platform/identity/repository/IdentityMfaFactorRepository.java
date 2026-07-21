package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentityMfaFactorEntity;
import ai.nova.platform.identity.entity.MfaFactorType;

public interface IdentityMfaFactorRepository extends JpaRepository<IdentityMfaFactorEntity, UUID> {

    Optional<IdentityMfaFactorEntity> findByIdentityUserIdAndFactorTypeAndEnabledTrue(
            UUID identityUserId, MfaFactorType factorType);

    List<IdentityMfaFactorEntity> findByIdentityUserId(UUID identityUserId);
}
