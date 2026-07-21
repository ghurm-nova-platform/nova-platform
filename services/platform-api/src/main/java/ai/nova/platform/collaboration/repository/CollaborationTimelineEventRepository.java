package ai.nova.platform.collaboration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.collaboration.entity.CollaborationTimelineEventEntity;

public interface CollaborationTimelineEventRepository extends JpaRepository<CollaborationTimelineEventEntity, UUID> {

    Optional<CollaborationTimelineEventEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<CollaborationTimelineEventEntity> findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID sessionId, UUID organizationId);
}
