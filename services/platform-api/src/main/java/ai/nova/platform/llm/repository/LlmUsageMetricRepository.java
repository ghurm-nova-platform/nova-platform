package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmUsageMetricEntity;

public interface LlmUsageMetricRepository extends JpaRepository<LlmUsageMetricEntity, UUID> {

    List<LlmUsageMetricEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

}
