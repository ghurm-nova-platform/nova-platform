package ai.nova.platform.knowledge.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.entity.ExecutionKnowledgeSnapshot;

public interface ExecutionKnowledgeSnapshotRepository extends JpaRepository<ExecutionKnowledgeSnapshot, UUID> {

    Optional<ExecutionKnowledgeSnapshot> findByExecutionIdAndProjectIdAndOrganizationId(
            UUID executionId, UUID projectId, UUID organizationId);
}
