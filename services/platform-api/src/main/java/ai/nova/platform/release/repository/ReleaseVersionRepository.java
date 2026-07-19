package ai.nova.platform.release.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.release.entity.ReleaseVersionEntity;

public interface ReleaseVersionRepository extends JpaRepository<ReleaseVersionEntity, UUID> {

    Optional<ReleaseVersionEntity> findByReleaseOperationId(UUID releaseOperationId);

    List<ReleaseVersionEntity> findByOrganizationIdAndProjectIdOrderByMajorVersionDescMinorVersionDescPatchVersionDesc(
            UUID organizationId, UUID projectId);
}
