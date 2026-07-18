package ai.nova.platform.patch.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "generated_patches")
public class GeneratedPatchEntity {

    @Id
    private UUID id;

    @Column(name = "patch_result_id", nullable = false)
    private UUID patchResultId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 1000)
    private String path;

    @Column(name = "old_path", length = 1000)
    private String oldPath;

    @Column(name = "new_path", length = 1000)
    private String newPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private PatchChangeType changeType;

    @Column(nullable = false)
    private int insertions;

    @Column(nullable = false)
    private int deletions;

    @Column(name = "patch_excerpt", columnDefinition = "TEXT")
    private String patchExcerpt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GeneratedPatchEntity() {
    }

    public GeneratedPatchEntity(
            UUID id,
            UUID patchResultId,
            UUID organizationId,
            String path,
            String oldPath,
            String newPath,
            PatchChangeType changeType,
            int insertions,
            int deletions,
            String patchExcerpt,
            Instant createdAt) {
        this.id = id;
        this.patchResultId = patchResultId;
        this.organizationId = organizationId;
        this.path = path;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.changeType = changeType;
        this.insertions = insertions;
        this.deletions = deletions;
        this.patchExcerpt = patchExcerpt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatchResultId() {
        return patchResultId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getPath() {
        return path;
    }

    public String getOldPath() {
        return oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public PatchChangeType getChangeType() {
        return changeType;
    }

    public int getInsertions() {
        return insertions;
    }

    public int getDeletions() {
        return deletions;
    }

    public String getPatchExcerpt() {
        return patchExcerpt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
