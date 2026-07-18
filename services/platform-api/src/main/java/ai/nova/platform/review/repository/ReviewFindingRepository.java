package ai.nova.platform.review.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.review.entity.ReviewFindingEntity;

public interface ReviewFindingRepository extends JpaRepository<ReviewFindingEntity, UUID> {

    List<ReviewFindingEntity> findByReviewResultIdOrderBySeverityDescTitleAsc(UUID reviewResultId);
}
