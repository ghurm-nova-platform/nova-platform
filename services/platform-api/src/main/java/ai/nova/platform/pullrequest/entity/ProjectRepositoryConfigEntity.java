package ai.nova.platform.pullrequest.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_repository_configs")
public class ProjectRepositoryConfigEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "repository_host", nullable = false, length = 255)
    private String repositoryHost;

    @Column(name = "repository_owner", nullable = false, length = 255)
    private String repositoryOwner;

    @Column(name = "repository_name", nullable = false, length = 255)
    private String repositoryName;

    @Column(name = "remote_url", nullable = false, length = 2000)
    private String remoteUrl;

    @Column(name = "target_base_ref", nullable = false, length = 255)
    private String targetBaseRef;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectRepositoryConfigEntity() {
    }

    public ProjectRepositoryConfigEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String provider,
            String repositoryHost,
            String repositoryOwner,
            String repositoryName,
            String remoteUrl,
            String targetBaseRef,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.provider = provider;
        this.repositoryHost = repositoryHost;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.remoteUrl = remoteUrl;
        this.targetBaseRef = targetBaseRef;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getProvider() {
        return provider;
    }

    public String getRepositoryHost() {
        return repositoryHost;
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getTargetBaseRef() {
        return targetBaseRef;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Test-only mutator for pointing project remotes at local bare repositories. */
    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    /** Test-only mutator for file:// remotes validated via localhost allowlist. */
    public void setRepositoryHost(String repositoryHost) {
        this.repositoryHost = repositoryHost;
    }
}
