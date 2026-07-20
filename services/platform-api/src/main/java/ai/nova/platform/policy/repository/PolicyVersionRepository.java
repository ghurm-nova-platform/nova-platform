package ai.nova.platform.policy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.policy.entity.PolicyVersionEntity;

public interface PolicyVersionRepository extends JpaRepository<PolicyVersionEntity, UUID> {

    List<PolicyVersionEntity> findByPolicyIdOrderByVersionNumberDesc(UUID policyId);

    Optional<PolicyVersionEntity> findFirstByPolicyIdOrderByVersionNumberDesc(UUID policyId);
}
