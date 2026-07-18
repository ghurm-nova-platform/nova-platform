package ai.nova.platform.review.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_findings")
public class ReviewFindingEntity {

    @Id
    private UUID id;

    @Column(name = "review_result_id", nullable = false)
    private UUID reviewResultId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReviewCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 4000)
    private String recommendation;

    @Column(name = "artifact_id")
    private UUID artifactId;

    @Column(name = "artifact_path", length = 1000)
    private String artifactPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReviewFindingEntity() {
    }

    public ReviewFindingEntity(
            UUID id,
            UUID reviewResultId,
            UUID organizationId,
            ReviewSeverity severity,
            ReviewCategory category,
            String title,
            String description,
            String recommendation,
            UUID artifactId,
            String artifactPath,
            Instant createdAt) {
        this.id = id;
        this.reviewResultId = reviewResultId;
        this.organizationId = organizationId;
        this.severity = severity;
        this.category = category;
        this.title = title;
        this.description = description;
        this.recommendation = recommendation;
        this.artifactId = artifactId;
        this.artifactPath = artifactPath;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReviewResultId() {
        return reviewResultId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public ReviewSeverity getSeverity() {
        return severity;
    }

    public ReviewCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public UUID getArtifactId() {
        return artifactId;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
