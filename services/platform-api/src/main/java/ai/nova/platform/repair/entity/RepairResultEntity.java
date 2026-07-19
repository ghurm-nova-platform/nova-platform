package ai.nova.platform.repair.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "repair_results")
public class RepairResultEntity {

    @Id
    private UUID id;

    @Column(name = "repair_operation_id", nullable = false)
    private UUID repairOperationId;

    @Column(name = "patch_result_id", nullable = false)
    private UUID patchResultId;

    @Column(name = "repaired_files_json", nullable = false, length = 8000)
    private String repairedFilesJson;

    @Column(nullable = false, length = 4000)
    private String summary;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RepairResultEntity() {
    }

    public RepairResultEntity(
            UUID id,
            UUID repairOperationId,
            UUID patchResultId,
            String repairedFilesJson,
            String summary,
            double confidence,
            Instant createdAt) {
        this.id = id;
        this.repairOperationId = repairOperationId;
        this.patchResultId = patchResultId;
        this.repairedFilesJson = repairedFilesJson;
        this.summary = summary;
        this.confidence = confidence;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRepairOperationId() {
        return repairOperationId;
    }

    public UUID getPatchResultId() {
        return patchResultId;
    }

    public String getRepairedFilesJson() {
        return repairedFilesJson;
    }

    public String getSummary() {
        return summary;
    }

    public double getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
