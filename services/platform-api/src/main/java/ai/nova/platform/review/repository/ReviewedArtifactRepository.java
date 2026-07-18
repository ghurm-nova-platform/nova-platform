package ai.nova.platform.review.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.review.entity.ReviewedArtifactEntity;

public interface ReviewedArtifactRepository extends JpaRepository<ReviewedArtifactEntity, UUID> {

    List<ReviewedArtifactEntity> findByReviewResultIdOrderByPathAsc(UUID reviewResultId);
}
