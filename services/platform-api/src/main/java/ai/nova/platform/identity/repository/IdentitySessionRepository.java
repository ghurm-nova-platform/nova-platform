package ai.nova.platform.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.entity.SessionStatus;

public interface IdentitySessionRepository extends JpaRepository<IdentitySessionEntity, UUID> {

    List<IdentitySessionEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            UUID organizationId, SessionStatus status);

    List<IdentitySessionEntity> findByIdentityUserIdAndStatus(UUID identityUserId, SessionStatus status);

    long countByIdentityUserIdAndStatus(UUID identityUserId, SessionStatus status);
}
