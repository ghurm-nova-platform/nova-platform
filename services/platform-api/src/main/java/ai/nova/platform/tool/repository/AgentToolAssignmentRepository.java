package ai.nova.platform.tool.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.tool.entity.AgentToolAssignment;

public interface AgentToolAssignmentRepository extends JpaRepository<AgentToolAssignment, UUID> {

    Optional<AgentToolAssignment> findByIdAndProjectIdAndOrganizationId(
            UUID id, UUID projectId, UUID organizationId);

    Optional<AgentToolAssignment> findByAgentIdAndToolIdAndProjectIdAndOrganizationId(
            UUID agentId, UUID toolId, UUID projectId, UUID organizationId);

    List<AgentToolAssignment> findByAgentIdAndProjectIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID agentId, UUID projectId, UUID organizationId);

    List<AgentToolAssignment> findByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrueOrderByCreatedAtAsc(
            UUID agentId, UUID projectId, UUID organizationId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AgentToolAssignment a
            SET a.enabled = false, a.updatedBy = :updatedBy, a.updatedAt = :updatedAt
            WHERE a.toolId = :toolId
              AND a.projectId = :projectId
              AND a.organizationId = :organizationId
              AND a.enabled = true
            """)
    int disableAllByToolId(
            @Param("toolId") UUID toolId,
            @Param("projectId") UUID projectId,
            @Param("organizationId") UUID organizationId,
            @Param("updatedBy") UUID updatedBy,
            @Param("updatedAt") Instant updatedAt);
}
