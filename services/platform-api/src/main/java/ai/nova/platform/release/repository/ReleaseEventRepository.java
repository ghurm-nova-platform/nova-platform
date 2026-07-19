package ai.nova.platform.release.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.release.entity.ReleaseEventEntity;

public interface ReleaseEventRepository extends JpaRepository<ReleaseEventEntity, UUID> {

    List<ReleaseEventEntity> findByReleaseOperationIdOrderByCreatedAtAsc(UUID releaseOperationId);
}
