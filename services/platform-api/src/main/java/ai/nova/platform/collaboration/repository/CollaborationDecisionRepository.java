package ai.nova.platform.collaboration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.collaboration.entity.CollaborationDecisionEntity;

public interface CollaborationDecisionRepository extends JpaRepository<CollaborationDecisionEntity, UUID> {

    Optional<CollaborationDecisionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<CollaborationDecisionEntity> findBySessionIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID sessionId, UUID organizationId);
}
