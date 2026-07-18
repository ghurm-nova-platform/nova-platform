package ai.nova.platform.review.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.review.entity.ReviewResultEntity;

public interface ReviewResultRepository extends JpaRepository<ReviewResultEntity, UUID> {

    Optional<ReviewResultEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ReviewResultEntity r WHERE r.taskId = :taskId AND r.organizationId = :organizationId")
    int deleteByTaskIdAndOrganizationId(
            @Param("taskId") UUID taskId, @Param("organizationId") UUID organizationId);
}
