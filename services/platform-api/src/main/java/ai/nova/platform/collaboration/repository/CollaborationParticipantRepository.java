package ai.nova.platform.collaboration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.collaboration.entity.CollaborationParticipantEntity;
import ai.nova.platform.collaboration.entity.CollaborationParticipantRole;

public interface CollaborationParticipantRepository extends JpaRepository<CollaborationParticipantEntity, UUID> {

    Optional<CollaborationParticipantEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<CollaborationParticipantEntity> findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID sessionId, UUID organizationId);

    Optional<CollaborationParticipantEntity> findBySessionIdAndOrganizationIdAndParticipantRole(
            UUID sessionId, UUID organizationId, CollaborationParticipantRole participantRole);
}
