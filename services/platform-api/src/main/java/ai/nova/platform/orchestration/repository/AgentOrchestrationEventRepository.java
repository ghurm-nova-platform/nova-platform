package ai.nova.platform.orchestration.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.orchestration.entity.AgentOrchestrationEvent;

public interface AgentOrchestrationEventRepository extends JpaRepository<AgentOrchestrationEvent, UUID> {

    List<AgentOrchestrationEvent> findByRunIdAndOrganizationIdOrderByEventSequenceAsc(
            UUID runId, UUID organizationId);

    @Query("""
            SELECT e FROM AgentOrchestrationEvent e
            WHERE e.runId = :runId
              AND e.organizationId = :organizationId
              AND (:taskId IS NULL OR e.taskId = :taskId)
            ORDER BY e.eventSequence ASC
            """)
    Page<AgentOrchestrationEvent> search(
            @Param("runId") UUID runId,
            @Param("organizationId") UUID organizationId,
            @Param("taskId") UUID taskId,
            Pageable pageable);
}
