package ai.nova.platform.release.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.release.entity.ReleaseOperationEntity;

public interface ReleaseOperationRepository extends JpaRepository<ReleaseOperationEntity, UUID> {

    Optional<ReleaseOperationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ReleaseOperationEntity> findByOrganizationIdAndProjectIdAndContentFingerprint(
            UUID organizationId, UUID projectId, String contentFingerprint);

    Optional<ReleaseOperationEntity> findByOrganizationIdAndProjectIdAndSemanticVersion(
            UUID organizationId, UUID projectId, String semanticVersion);

    List<ReleaseOperationEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);

    List<ReleaseOperationEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    @Query("""
            SELECT COALESCE(MAX(r.releaseNumber), 0)
            FROM ReleaseOperationEntity r
            WHERE r.organizationId = :organizationId AND r.projectId = :projectId
            """)
    long findMaxReleaseNumber(@Param("organizationId") UUID organizationId, @Param("projectId") UUID projectId);
}
