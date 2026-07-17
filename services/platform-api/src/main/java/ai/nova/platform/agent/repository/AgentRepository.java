package ai.nova.platform.agent.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.entity.AgentStatus;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    Optional<Agent> findByIdAndProjectIdAndOrganizationId(UUID id, UUID projectId, UUID organizationId);

    boolean existsByPromptIdAndStatus(UUID promptId, AgentStatus status);

    @Query("""
            SELECT a FROM Agent a
            WHERE a.organizationId = :organizationId
              AND a.projectId = :projectId
              AND (:status IS NULL OR a.status = :status)
              AND (
                   :search IS NULL
                   OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              )
            """)
    Page<Agent> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            @Param("status") AgentStatus status,
            Pageable pageable);
}
