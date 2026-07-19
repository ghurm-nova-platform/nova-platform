package ai.nova.platform.release.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.release.entity.ReleaseArtifactEntity;

public interface ReleaseArtifactRepository extends JpaRepository<ReleaseArtifactEntity, UUID> {

    List<ReleaseArtifactEntity> findByReleaseOperationIdOrderByCreatedAtAsc(UUID releaseOperationId);
}
