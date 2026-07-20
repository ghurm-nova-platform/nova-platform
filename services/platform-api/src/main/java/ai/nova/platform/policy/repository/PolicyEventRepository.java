package ai.nova.platform.policy.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.policy.entity.PolicyEventEntity;

public interface PolicyEventRepository extends JpaRepository<PolicyEventEntity, UUID> {

    List<PolicyEventEntity> findByPolicyIdOrderByCreatedAtAsc(UUID policyId);
}
