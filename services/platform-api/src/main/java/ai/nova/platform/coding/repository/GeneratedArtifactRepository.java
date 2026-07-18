package ai.nova.platform.coding.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.coding.entity.GeneratedArtifact;

public interface GeneratedArtifactRepository extends JpaRepository<GeneratedArtifact, UUID> {

    List<GeneratedArtifact> findByTaskIdAndOrganizationIdOrderByPathAsc(UUID taskId, UUID organizationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GeneratedArtifact a WHERE a.taskId = :taskId AND a.organizationId = :organizationId")
    int deleteByTaskIdAndOrganizationId(
            @Param("taskId") UUID taskId, @Param("organizationId") UUID organizationId);
}
