package ai.nova.platform.ci.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.ci.entity.CiObservationOperationEntity;

public interface CiObservationOperationRepository extends JpaRepository<CiObservationOperationEntity, UUID> {

    Optional<CiObservationOperationEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    List<CiObservationOperationEntity> findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);
}
