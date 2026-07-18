package ai.nova.platform.patch.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.patch.entity.PatchResultEntity;

public interface PatchResultRepository extends JpaRepository<PatchResultEntity, UUID> {

    Optional<PatchResultEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PatchResultEntity r WHERE r.taskId = :taskId AND r.organizationId = :organizationId")
    void deleteByTaskIdAndOrganizationId(
            @Param("taskId") UUID taskId, @Param("organizationId") UUID organizationId);
}
