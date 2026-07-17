package ai.nova.platform.knowledge.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_embeddings")
public class KnowledgeEmbedding {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "knowledge_base_id", nullable = false)
    private UUID knowledgeBaseId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "provider_key", nullable = false, length = 100)
    private String providerKey;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer dimensions;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "embedding_hash", nullable = false, length = 64)
    private String embeddingHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeEmbedding() {
    }

    public KnowledgeEmbedding(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            UUID documentId,
            UUID chunkId,
            String providerKey,
            String model,
            Integer dimensions,
            String embedding,
            String embeddingHash,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.documentId = documentId;
        this.chunkId = chunkId;
        this.providerKey = providerKey;
        this.model = model;
        this.dimensions = dimensions;
        this.embedding = embedding;
        this.embeddingHash = embeddingHash;
        this.createdAt = createdAt;
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

    public UUID getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getChunkId() {
        return chunkId;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public String getModel() {
        return model;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public String getEmbedding() {
        return embedding;
    }

    public String getEmbeddingHash() {
        return embeddingHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
