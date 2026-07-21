package ai.nova.platform.collaboration.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.collaboration.entity.CollaborationTaskEntity;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;

public interface CollaborationTaskRepository extends JpaRepository<CollaborationTaskEntity, UUID> {

    Optional<CollaborationTaskEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<CollaborationTaskEntity> findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID sessionId, UUID organizationId);

    List<CollaborationTaskEntity> findBySessionIdAndOrganizationIdAndStatusIn(
            UUID sessionId, UUID organizationId, Collection<CollaborationTaskStatus> statuses);

    List<CollaborationTaskEntity> findBySessionIdAndOrganizationIdAndArtifactRefAndStatusIn(
            UUID sessionId,
            UUID organizationId,
            String artifactRef,
            Collection<CollaborationTaskStatus> statuses);

    List<CollaborationTaskEntity> findBySessionIdAndOrganizationIdAndParallelGroup(
            UUID sessionId, UUID organizationId, String parallelGroup);

    List<CollaborationTaskEntity> findBySessionIdAndOrganizationIdAndParticipantIdAndStatusIn(
            UUID sessionId,
            UUID organizationId,
            UUID participantId,
            Collection<CollaborationTaskStatus> statuses);

    List<CollaborationTaskEntity> findBySessionIdAndOrganizationIdAndParticipantId(
            UUID sessionId, UUID organizationId, UUID participantId);

    boolean existsBySessionIdAndOrganizationIdAndParticipantIdAndStatusIn(
            UUID sessionId,
            UUID organizationId,
            UUID participantId,
            Collection<CollaborationTaskStatus> statuses);
}
