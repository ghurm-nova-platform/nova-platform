package ai.nova.platform.testing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.testing.entity.GeneratedTestEntity;

public interface GeneratedTestRepository extends JpaRepository<GeneratedTestEntity, UUID> {

    List<GeneratedTestEntity> findByTestingResultIdOrderByPriorityDescTitleAsc(UUID testingResultId);
}
