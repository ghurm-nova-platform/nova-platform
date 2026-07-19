package ai.nova.platform.merge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.merge.entity.MergeEventEntity;

public interface MergeEventRepository extends JpaRepository<MergeEventEntity, UUID> {

    List<MergeEventEntity> findByMergeOperationIdOrderByCreatedAtAsc(UUID mergeOperationId);
}
