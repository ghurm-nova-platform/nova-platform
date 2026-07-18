package ai.nova.platform.testing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.testing.entity.GeneratedTestCaseEntity;

public interface GeneratedTestCaseRepository extends JpaRepository<GeneratedTestCaseEntity, UUID> {

    List<GeneratedTestCaseEntity> findByTestingResultIdOrderByNameAsc(UUID testingResultId);

    List<GeneratedTestCaseEntity> findByGeneratedTestIdOrderByNameAsc(UUID generatedTestId);
}
