package ai.nova.platform.prompt.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.prompt.entity.PromptVersion;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, UUID> {

    List<PromptVersion> findByPromptIdOrderByVersionNumberDesc(UUID promptId);

    @Query("""
            SELECT COALESCE(MAX(v.versionNumber), 0)
            FROM PromptVersion v
            WHERE v.promptId = :promptId
            """)
    int findMaxVersionNumber(@Param("promptId") UUID promptId);

    Optional<PromptVersion> findByIdAndPromptIdAndOrganizationIdAndProjectId(
            UUID id, UUID promptId, UUID organizationId, UUID projectId);
}
