package ai.nova.platform.ci.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.ci.entity.CiJobEntity;

public interface CiJobRepository extends JpaRepository<CiJobEntity, UUID> {

    List<CiJobEntity> findByCiWorkflowRunIdOrderByCreatedAtAsc(UUID ciWorkflowRunId);
}
