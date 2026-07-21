package ai.nova.platform.collaboration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.collaboration.entity.CollaborationMessageEntity;

public interface CollaborationMessageRepository extends JpaRepository<CollaborationMessageEntity, UUID> {

    Optional<CollaborationMessageEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<CollaborationMessageEntity> findBySessionIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID sessionId, UUID organizationId, Pageable pageable);

    List<CollaborationMessageEntity> findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID sessionId, UUID organizationId);

    long countBySessionIdAndOrganizationId(UUID sessionId, UUID organizationId);
}
