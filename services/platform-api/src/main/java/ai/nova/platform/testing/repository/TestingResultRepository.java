package ai.nova.platform.testing.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.testing.entity.TestingResultEntity;

public interface TestingResultRepository extends JpaRepository<TestingResultEntity, UUID> {

    Optional<TestingResultEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestingResultEntity r WHERE r.taskId = :taskId AND r.organizationId = :organizationId")
    int deleteByTaskIdAndOrganizationId(
            @Param("taskId") UUID taskId, @Param("organizationId") UUID organizationId);
}
