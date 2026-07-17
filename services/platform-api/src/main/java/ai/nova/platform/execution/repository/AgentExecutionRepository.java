package ai.nova.platform.execution.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionStatus;

public interface AgentExecutionRepository extends JpaRepository<AgentExecution, UUID> {

    Optional<AgentExecution> findByIdAndProjectIdAndOrganizationId(
            UUID id, UUID projectId, UUID organizationId);

    long countByConversationId(UUID conversationId);

    java.util.List<AgentExecution> findByConversationId(UUID conversationId);

    @Query("""
            SELECT e FROM AgentExecution e
            WHERE e.organizationId = :organizationId
              AND e.projectId = :projectId
              AND (:agentId IS NULL OR e.agentId = :agentId)
              AND (:status IS NULL OR e.status = :status)
            """)
    Page<AgentExecution> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("agentId") UUID agentId,
            @Param("status") ExecutionStatus status,
            Pageable pageable);
}
