package ai.nova.platform.prreview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.prreview.entity.PrReviewRecommendationEntity;

public interface PrReviewRecommendationRepository extends JpaRepository<PrReviewRecommendationEntity, UUID> {

    Optional<PrReviewRecommendationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<PrReviewRecommendationEntity> findByReviewRunIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID reviewRunId, UUID organizationId);
}
