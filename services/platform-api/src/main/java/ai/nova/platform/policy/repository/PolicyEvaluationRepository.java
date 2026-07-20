package ai.nova.platform.policy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.policy.entity.PolicyEvaluationEntity;

public interface PolicyEvaluationRepository extends JpaRepository<PolicyEvaluationEntity, UUID> {

    Optional<PolicyEvaluationEntity> findByOrganizationIdAndEvaluationHash(UUID organizationId, String evaluationHash);

    List<PolicyEvaluationEntity> findByPolicyIdOrderByCreatedAtDesc(UUID policyId);

    Optional<PolicyEvaluationEntity> findFirstByPolicyIdOrderByCreatedAtDesc(UUID policyId);

    Optional<PolicyEvaluationEntity> findFirstByPolicyIdOrderByEvaluatedAtDesc(UUID policyId);
}
