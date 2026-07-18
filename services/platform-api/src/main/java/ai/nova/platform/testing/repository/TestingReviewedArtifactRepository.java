package ai.nova.platform.testing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.testing.entity.TestingReviewedArtifactEntity;

public interface TestingReviewedArtifactRepository extends JpaRepository<TestingReviewedArtifactEntity, UUID> {

    List<TestingReviewedArtifactEntity> findByTestingResultIdOrderByPathAsc(UUID testingResultId);
}
