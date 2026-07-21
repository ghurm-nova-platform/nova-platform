package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmModelVersionEntity;

public interface LlmModelVersionRepository extends JpaRepository<LlmModelVersionEntity, UUID> {

    List<LlmModelVersionEntity> findByModelIdOrderByCreatedAtDesc(UUID modelId);

}
