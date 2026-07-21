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
@Table(name = "pr_review_findings")
public class PrReviewFindingEntity {

    @Id
    private UUID id;

    @Column(name = "review_run_id", nullable = false)
    private UUID reviewRunId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private ReviewCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ReviewSeverity severity;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommendation", nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "line_hint")
    private Integer lineHint;

    @Column(name = "references_json", columnDefinition = "TEXT")
    private String referencesJson;

    @Column(name = "knowledge_document_ids_json", columnDefinition = "TEXT")
    private String knowledgeDocumentIdsJson;

    @Column(name = "rule_code", length = 100)
    private String ruleCode;

    @Column(name = "evidence_excerpt", columnDefinition = "TEXT")
    private String evidenceExcerpt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PrReviewFindingEntity() {
    }

    public PrReviewFindingEntity(
            UUID id,
            UUID reviewRunId,
            UUID organizationId,
            ReviewCategory category,
            ReviewSeverity severity,
            String title,
            String description,
            String recommendation,
            String filePath,
            Integer lineHint,
            String referencesJson,
            String knowledgeDocumentIdsJson,
            Instant createdAt) {
        this(
                id,
                reviewRunId,
                organizationId,
                category,
                severity,
                title,
                description,
                recommendation,
                filePath,
                lineHint,
                referencesJson,
                knowledgeDocumentIdsJson,
                null,
                null,
                createdAt);
    }

    public PrReviewFindingEntity(
            UUID id,
            UUID reviewRunId,
            UUID organizationId,
            ReviewCategory category,
            ReviewSeverity severity,
            String title,
            String description,
            String recommendation,
            String filePath,
            Integer lineHint,
            String referencesJson,
            String knowledgeDocumentIdsJson,
            String ruleCode,
            String evidenceExcerpt,
            Instant createdAt) {
        this.id = id;
        this.reviewRunId = reviewRunId;
        this.organizationId = organizationId;
        this.category = category;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.recommendation = recommendation;
        this.filePath = filePath;
        this.lineHint = lineHint;
        this.referencesJson = referencesJson;
        this.knowledgeDocumentIdsJson = knowledgeDocumentIdsJson;
        this.ruleCode = ruleCode;
        this.evidenceExcerpt = evidenceExcerpt;
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

    public ReviewCategory getCategory() {
        return category;
    }

    public ReviewSeverity getSeverity() {
        return severity;
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

    public String getFilePath() {
        return filePath;
    }

    public Integer getLineHint() {
        return lineHint;
    }

    public String getReferencesJson() {
        return referencesJson;
    }

    public String getKnowledgeDocumentIdsJson() {
        return knowledgeDocumentIdsJson;
    }

    public void setKnowledgeDocumentIdsJson(String knowledgeDocumentIdsJson) {
        this.knowledgeDocumentIdsJson = knowledgeDocumentIdsJson;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getEvidenceExcerpt() {
        return evidenceExcerpt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
