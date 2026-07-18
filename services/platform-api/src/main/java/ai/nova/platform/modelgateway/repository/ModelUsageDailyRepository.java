package ai.nova.platform.modelgateway.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.modelgateway.entity.ModelUsageDaily;

import jakarta.persistence.LockModeType;

public interface ModelUsageDailyRepository extends JpaRepository<ModelUsageDaily, UUID> {

    Optional<ModelUsageDaily> findByProjectIdAndModelIdAndUsageDate(
            UUID projectId, UUID modelId, LocalDate usageDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u FROM ModelUsageDaily u
            WHERE u.projectId = :projectId AND u.modelId = :modelId AND u.usageDate = :usageDate
            """)
    Optional<ModelUsageDaily> findForUpdate(
            @Param("projectId") UUID projectId,
            @Param("modelId") UUID modelId,
            @Param("usageDate") LocalDate usageDate);

    List<ModelUsageDaily> findByProjectIdAndOrganizationIdAndUsageDateBetweenOrderByUsageDateDescModelIdAsc(
            UUID projectId, UUID organizationId, LocalDate from, LocalDate to);
}
