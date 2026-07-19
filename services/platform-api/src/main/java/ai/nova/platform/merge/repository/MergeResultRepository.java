package ai.nova.platform.merge.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.merge.entity.MergeResultEntity;

public interface MergeResultRepository extends JpaRepository<MergeResultEntity, UUID> {

    Optional<MergeResultEntity> findByMergeOperationId(UUID mergeOperationId);
}
