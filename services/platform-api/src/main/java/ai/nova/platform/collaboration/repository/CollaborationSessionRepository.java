package ai.nova.platform.collaboration.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.collaboration.entity.CollaborationSessionEntity;

import jakarta.persistence.LockModeType;

import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;

public interface CollaborationSessionRepository extends JpaRepository<CollaborationSessionEntity, UUID> {

    Optional<CollaborationSessionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CollaborationSessionEntity s WHERE s.id = :id AND s.organizationId = :organizationId")
    Optional<CollaborationSessionEntity> findByIdAndOrganizationIdForUpdate(
            @Param("id") UUID id, @Param("organizationId") UUID organizationId);

    List<CollaborationSessionEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<CollaborationSessionEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);

    List<CollaborationSessionEntity> findByOrganizationIdAndOrchestrationRunIdOrderByCreatedAtDesc(
            UUID organizationId, UUID orchestrationRunId);

    List<CollaborationSessionEntity> findByOrganizationIdAndStatusInOrderByCreatedAtDesc(
            UUID organizationId, List<CollaborationSessionStatus> statuses);
}
