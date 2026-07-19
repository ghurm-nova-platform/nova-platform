package ai.nova.platform.ci.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.ci.entity.CiWorkflowRunEntity;

public interface CiWorkflowRunRepository extends JpaRepository<CiWorkflowRunEntity, UUID> {

    List<CiWorkflowRunEntity> findByCiObservationOperationIdOrderByCreatedAtAsc(UUID ciObservationOperationId);
}
