package ai.nova.platform.knowledge.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "knowledge_bases")
public class KnowledgeBase {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "knowledge_key", nullable = false, length = 100)
    private String knowledgeKey;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KnowledgeBaseStatus status;

    @Column(name = "embedding_provider_key", nullable = false, length = 100)
    private String embeddingProviderKey;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(name = "embedding_dimensions", nullable = false)
    private Integer embeddingDimensions;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    @Column(name = "chunk_overlap", nullable = false)
    private Integer chunkOverlap;

    @Column(name = "default_top_k", nullable = false)
    private Integer defaultTopK;

    @Column(name = "minimum_score", precision = 8, scale = 6)
    private BigDecimal minimumScore;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    protected KnowledgeBase() {
    }

    public KnowledgeBase(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String knowledgeKey,
            String name,
            String description,
            KnowledgeBaseStatus status,
            String embeddingProviderKey,
            String embeddingModel,
            Integer embeddingDimensions,
            Integer chunkSize,
            Integer chunkOverlap,
            Integer defaultTopK,
            BigDecimal minimumScore,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.knowledgeKey = knowledgeKey;
        this.name = name;
        this.description = description;
        this.status = status;
        this.embeddingProviderKey = embeddingProviderKey;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.defaultTopK = defaultTopK;
        this.minimumScore = minimumScore;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
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

    public String getKnowledgeKey() {
        return knowledgeKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public KnowledgeBaseStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeBaseStatus status) {
        this.status = status;
    }

    public String getEmbeddingProviderKey() {
        return embeddingProviderKey;
    }

    public void setEmbeddingProviderKey(String embeddingProviderKey) {
        this.embeddingProviderKey = embeddingProviderKey;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(Integer embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Integer getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(Integer defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public BigDecimal getMinimumScore() {
        return minimumScore;
    }

    public void setMinimumScore(BigDecimal minimumScore) {
        this.minimumScore = minimumScore;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
