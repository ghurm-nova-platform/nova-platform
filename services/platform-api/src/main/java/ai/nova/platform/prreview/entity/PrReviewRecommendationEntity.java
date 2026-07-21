package ai.nova.platform.prreview.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pr_review_recommendations")
public class PrReviewRecommendationEntity {

    @Id
    private UUID id;

    @Column(name = "review_run_id", nullable = false)
    private UUID reviewRunId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "finding_id")
    private UUID findingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private RecommendationPriority priority;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "knowledge_document_ids_json", columnDefinition = "TEXT")
    private String knowledgeDocumentIdsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PrReviewRecommendationEntity() {
    }

    public PrReviewRecommendationEntity(
            UUID id,
            UUID reviewRunId,
            UUID organizationId,
            UUID findingId,
            RecommendationPriority priority,
            String title,
            String description,
            String knowledgeDocumentIdsJson,
            Instant createdAt) {
        this.id = id;
        this.reviewRunId = reviewRunId;
        this.organizationId = organizationId;
        this.findingId = findingId;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.knowledgeDocumentIdsJson = knowledgeDocumentIdsJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReviewRunId() {
        return reviewRunId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getFindingId() {
        return findingId;
    }

    public RecommendationPriority getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getKnowledgeDocumentIdsJson() {
        return knowledgeDocumentIdsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
