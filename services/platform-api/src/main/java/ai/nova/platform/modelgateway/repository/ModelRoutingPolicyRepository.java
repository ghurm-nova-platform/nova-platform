package ai.nova.platform.modelgateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.modelgateway.entity.ModelRoutingPolicy;
import ai.nova.platform.modelgateway.entity.RoutingPolicyStatus;

public interface ModelRoutingPolicyRepository extends JpaRepository<ModelRoutingPolicy, UUID> {

    Optional<ModelRoutingPolicy> findByIdAndProjectIdAndOrganizationId(
            UUID id, UUID projectId, UUID organizationId);

    boolean existsByProjectIdAndAgentIdAndPolicyKey(UUID projectId, UUID agentId, String policyKey);

    @Query("""
            SELECT p FROM ModelRoutingPolicy p
            WHERE p.organizationId = :organizationId
              AND p.projectId = :projectId
              AND (:status IS NULL OR p.status = :status)
              AND (:agentId IS NULL OR p.agentId = :agentId)
            """)
    Page<ModelRoutingPolicy> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("agentId") UUID agentId,
            @Param("status") RoutingPolicyStatus status,
            Pageable pageable);

    Optional<ModelRoutingPolicy> findFirstByProjectIdAndAgentIdAndOrganizationIdAndStatusOrderByUpdatedAtDesc(
            UUID projectId, UUID agentId, UUID organizationId, RoutingPolicyStatus status);

    Optional<ModelRoutingPolicy> findFirstByProjectIdAndAgentIdIsNullAndOrganizationIdAndStatusOrderByUpdatedAtDesc(
            UUID projectId, UUID organizationId, RoutingPolicyStatus status);

    List<ModelRoutingPolicy> findByProjectIdAndOrganizationIdAndStatus(
            UUID projectId, UUID organizationId, RoutingPolicyStatus status);
}
