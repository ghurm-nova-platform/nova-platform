package ai.nova.platform.release.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.release.entity.ReleaseContentEntity;

public interface ReleaseContentRepository extends JpaRepository<ReleaseContentEntity, UUID> {

    List<ReleaseContentEntity> findByReleaseOperationIdOrderBySortOrderAsc(UUID releaseOperationId);
}
