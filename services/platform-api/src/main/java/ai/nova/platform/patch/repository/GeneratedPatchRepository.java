package ai.nova.platform.patch.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.patch.entity.GeneratedPatchEntity;

public interface GeneratedPatchRepository extends JpaRepository<GeneratedPatchEntity, UUID> {

    List<GeneratedPatchEntity> findByPatchResultIdOrderByPathAsc(UUID patchResultId);
}
