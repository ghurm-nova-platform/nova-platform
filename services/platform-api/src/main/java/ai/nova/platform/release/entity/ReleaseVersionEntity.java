package ai.nova.platform.release.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "release_versions")
public class ReleaseVersionEntity {

    @Id
    private UUID id;

    @Column(name = "release_operation_id", nullable = false)
    private UUID releaseOperationId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "semantic_version", nullable = false, length = 64)
    private String semanticVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_strategy", nullable = false, length = 30)
    private VersionStrategy versionStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "bump_type", length = 20)
    private VersionBump bumpType;

    @Column(name = "major_version", nullable = false)
    private int majorVersion;

    @Column(name = "minor_version", nullable = false)
    private int minorVersion;

    @Column(name = "patch_version", nullable = false)
    private int patchVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReleaseVersionEntity() {
    }

    public ReleaseVersionEntity(
            UUID id,
            UUID releaseOperationId,
            UUID organizationId,
            UUID projectId,
            String semanticVersion,
            VersionStrategy versionStrategy,
            VersionBump bumpType,
            int majorVersion,
            int minorVersion,
            int patchVersion,
            Instant createdAt) {
        this.id = id;
        this.releaseOperationId = releaseOperationId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.semanticVersion = semanticVersion;
        this.versionStrategy = versionStrategy;
        this.bumpType = bumpType;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReleaseOperationId() {
        return releaseOperationId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public VersionStrategy getVersionStrategy() {
        return versionStrategy;
    }

    public VersionBump getBumpType() {
        return bumpType;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getPatchVersion() {
        return patchVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
