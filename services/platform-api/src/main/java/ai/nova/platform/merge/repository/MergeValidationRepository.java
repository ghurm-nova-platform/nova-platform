package ai.nova.platform.merge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.merge.entity.MergeValidationEntity;

public interface MergeValidationRepository extends JpaRepository<MergeValidationEntity, UUID> {

    List<MergeValidationEntity> findByMergeOperationIdOrderByEvaluatedAtAsc(UUID mergeOperationId);

    void deleteByMergeOperationId(UUID mergeOperationId);
}
