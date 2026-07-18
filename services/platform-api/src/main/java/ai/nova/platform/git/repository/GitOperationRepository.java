package ai.nova.platform.git.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.git.entity.GitOperationEntity;

public interface GitOperationRepository extends JpaRepository<GitOperationEntity, UUID> {

    Optional<GitOperationEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    boolean existsByOrganizationIdAndProjectIdAndBranchName(
            UUID organizationId, UUID projectId, String branchName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GitOperationEntity g WHERE g.taskId = :taskId AND g.organizationId = :organizationId")
    void deleteByTaskIdAndOrganizationId(
            @Param("taskId") UUID taskId, @Param("organizationId") UUID organizationId);
}
