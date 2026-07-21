package ai.nova.platform.collaboration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.collaboration.entity.CollaborationSessionEntity;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;

public interface CollaborationSessionRepository extends JpaRepository<CollaborationSessionEntity, UUID> {

    Optional<CollaborationSessionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<CollaborationSessionEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<CollaborationSessionEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);

    List<CollaborationSessionEntity> findByOrganizationIdAndOrchestrationRunIdOrderByCreatedAtDesc(
            UUID organizationId, UUID orchestrationRunId);

    List<CollaborationSessionEntity> findByOrganizationIdAndStatusInOrderByCreatedAtDesc(
            UUID organizationId, List<CollaborationSessionStatus> statuses);
}
