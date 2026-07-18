package ai.nova.platform.patch.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.patch.entity.PatchArtifactEntity;

public interface PatchArtifactRepository extends JpaRepository<PatchArtifactEntity, UUID> {

    List<PatchArtifactEntity> findByPatchResultIdOrderByPathAsc(UUID patchResultId);
}
