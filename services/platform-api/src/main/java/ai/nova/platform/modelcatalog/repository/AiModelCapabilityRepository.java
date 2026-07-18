package ai.nova.platform.modelcatalog.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity.AiModelCapabilityId;

public interface AiModelCapabilityRepository extends JpaRepository<AiModelCapabilityEntity, AiModelCapabilityId> {

    List<AiModelCapabilityEntity> findByIdModelId(UUID modelId);

    List<AiModelCapabilityEntity> findByIdModelIdIn(Collection<UUID> modelIds);

    boolean existsByIdModelIdAndEnabledTrue(UUID modelId);

    boolean existsByIdModelIdAndIdCapabilityAndEnabledTrue(UUID modelId, AiModelCapability capability);

    void deleteByIdModelId(UUID modelId);
}
