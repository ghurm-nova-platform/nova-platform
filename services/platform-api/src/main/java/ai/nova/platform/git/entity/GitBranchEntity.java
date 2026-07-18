package ai.nova.platform.git.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "git_branches")
public class GitBranchEntity {

    @Id
    private UUID id;

    @Column(name = "git_operation_id", nullable = false)
    private UUID gitOperationId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Column(name = "base_ref", nullable = false, length = 255)
    private String baseRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GitBranchEntity() {
    }

    public GitBranchEntity(
            UUID id,
            UUID gitOperationId,
            UUID organizationId,
            String branchName,
            String baseRef,
            Instant createdAt) {
        this.id = id;
        this.gitOperationId = gitOperationId;
        this.organizationId = organizationId;
        this.branchName = branchName;
        this.baseRef = baseRef;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGitOperationId() {
        return gitOperationId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getBaseRef() {
        return baseRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
