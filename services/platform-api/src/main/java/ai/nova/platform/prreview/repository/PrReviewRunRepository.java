package ai.nova.platform.prreview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.prreview.entity.PrReviewRunEntity;

public interface PrReviewRunRepository extends JpaRepository<PrReviewRunEntity, UUID> {

    Optional<PrReviewRunEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<PrReviewRunEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<PrReviewRunEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);

    List<PrReviewRunEntity> findByOrganizationIdAndPullRequestOperationIdOrderByCreatedAtDesc(
            UUID organizationId, UUID pullRequestOperationId);

    List<PrReviewRunEntity> findByOrganizationIdAndPullRequestNumberOrderByCreatedAtDesc(
            UUID organizationId, Integer pullRequestNumber);
}
