package ai.nova.platform.policy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.policy.entity.PolicyStatus;
import ai.nova.platform.policy.entity.ReleasePolicyEntity;

public interface ReleasePolicyRepository extends JpaRepository<ReleasePolicyEntity, UUID> {

    Optional<ReleasePolicyEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ReleasePolicyEntity> findByOrganizationIdAndPolicyFingerprint(UUID organizationId, String fingerprint);

    List<ReleasePolicyEntity> findByOrganizationIdOrderByPriorityAscCreatedAtDesc(UUID organizationId);

    List<ReleasePolicyEntity> findByOrganizationIdAndProjectIdOrderByPriorityAscCreatedAtDesc(
            UUID organizationId, UUID projectId);

    List<ReleasePolicyEntity> findByOrganizationIdAndProjectIdAndStatusOrderByPriorityAscCreatedAtDesc(
            UUID organizationId, UUID projectId, PolicyStatus status);
}
