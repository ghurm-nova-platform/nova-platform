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
@Table(name = "release_operations")
public class ReleaseOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "release_number", nullable = false)
    private long releaseNumber;

    @Column(name = "semantic_version", nullable = false, length = 64)
    private String semanticVersion;

    @Column(name = "release_name", nullable = false, length = 255)
    private String releaseName;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReleaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_strategy", nullable = false, length = 30)
    private VersionStrategy versionStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "bump_type", length = 20)
    private VersionBump bumpType;

    @Column(name = "content_fingerprint", nullable = false, length = 64)
    private String contentFingerprint;

    @Column(name = "manifest_json", columnDefinition = "TEXT")
    private String manifestJson;

    @Column(name = "manifest_hash", length = 64)
    private String manifestHash;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "prepared_at")
    private Instant preparedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReleaseOperationEntity() {
    }

    public ReleaseOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            long releaseNumber,
            String semanticVersion,
            String releaseName,
            String description,
            ReleaseStatus status,
            VersionStrategy versionStrategy,
            VersionBump bumpType,
            String contentFingerprint,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.releaseNumber = releaseNumber;
        this.semanticVersion = semanticVersion;
        this.releaseName = releaseName;
        this.description = description;
        this.status = status;
        this.versionStrategy = versionStrategy;
        this.bumpType = bumpType;
        this.contentFingerprint = contentFingerprint;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public long getReleaseNumber() {
        return releaseNumber;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getDescription() {
        return description;
    }

    public ReleaseStatus getStatus() {
        return status;
    }

    public void setStatus(ReleaseStatus status) {
        this.status = status;
    }

    public VersionStrategy getVersionStrategy() {
        return versionStrategy;
    }

    public VersionBump getBumpType() {
        return bumpType;
    }

    public String getContentFingerprint() {
        return contentFingerprint;
    }

    public String getManifestJson() {
        return manifestJson;
    }

    public void setManifestJson(String manifestJson) {
        this.manifestJson = manifestJson;
    }

    public String getManifestHash() {
        return manifestHash;
    }

    public void setManifestHash(String manifestHash) {
        this.manifestHash = manifestHash;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getPreparedAt() {
        return preparedAt;
    }

    public void setPreparedAt(Instant preparedAt) {
        this.preparedAt = preparedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
