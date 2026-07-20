package ai.nova.platform.policy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.policy.entity.PolicyEvidenceEntity;

public interface PolicyEvidenceRepository extends JpaRepository<PolicyEvidenceEntity, UUID> {

    List<PolicyEvidenceEntity> findByPolicyEvaluationIdOrderByCreatedAtAsc(UUID policyEvaluationId);

    Optional<PolicyEvidenceEntity> findByPolicyEvaluationIdAndEvidenceKey(UUID policyEvaluationId, String evidenceKey);
}
