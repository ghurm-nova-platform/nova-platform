package ai.nova.platform.prreview.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.prreview.entity.PrReviewFindingEntity;

public interface PrReviewFindingRepository extends JpaRepository<PrReviewFindingEntity, UUID> {

    Optional<PrReviewFindingEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<PrReviewFindingEntity> findByReviewRunIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID reviewRunId, UUID organizationId);
}
