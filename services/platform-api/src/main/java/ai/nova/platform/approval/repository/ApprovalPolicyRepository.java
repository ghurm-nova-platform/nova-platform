package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.entity.ApprovalPolicyStatus;

public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicyEntity, UUID> {

    List<ApprovalPolicyEntity> findByOrganizationIdOrderByNameAscVersionDesc(UUID organizationId);

    Optional<ApprovalPolicyEntity> findFirstByOrganizationIdAndProjectIdAndIsDefaultTrueAndStatusOrderByVersionDesc(
            UUID organizationId, UUID projectId, ApprovalPolicyStatus status);

    Optional<ApprovalPolicyEntity> findFirstByOrganizationIdAndProjectIdIsNullAndIsDefaultTrueAndStatusOrderByVersionDesc(
            UUID organizationId, ApprovalPolicyStatus status);

    Optional<ApprovalPolicyEntity> findFirstByOrganizationIdAndProjectIdAndNameOrderByVersionDesc(
            UUID organizationId, UUID projectId, String name);

    Optional<ApprovalPolicyEntity> findFirstByOrganizationIdAndProjectIdIsNullAndNameOrderByVersionDesc(
            UUID organizationId, String name);

    Optional<ApprovalPolicyEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
