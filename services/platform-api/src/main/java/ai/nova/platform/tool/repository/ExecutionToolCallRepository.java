package ai.nova.platform.tool.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.tool.entity.ExecutionToolCall;
import ai.nova.platform.tool.entity.ToolCallStatus;

public interface ExecutionToolCallRepository extends JpaRepository<ExecutionToolCall, UUID> {

    Optional<ExecutionToolCall> findByIdAndProjectIdAndOrganizationId(
            UUID id, UUID projectId, UUID organizationId);

    Optional<ExecutionToolCall> findByExecutionIdAndRuntimeCallId(
            UUID executionId, String runtimeCallId);

    List<ExecutionToolCall> findByExecutionIdAndProjectIdAndOrganizationIdOrderBySequenceNumberAsc(
            UUID executionId, UUID projectId, UUID organizationId);

    @Query("""
            SELECT COALESCE(MAX(c.sequenceNumber), 0)
            FROM ExecutionToolCall c
            WHERE c.executionId = :executionId
            """)
    int findMaxSequenceNumber(@Param("executionId") UUID executionId);

    long countByExecutionIdAndStatusIn(UUID executionId, List<ToolCallStatus> statuses);
}
