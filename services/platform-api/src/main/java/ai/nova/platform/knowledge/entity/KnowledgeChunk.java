package ai.nova.platform.knowledge.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_chunks")
public class KnowledgeChunk {

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

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "character_start", nullable = false)
    private Integer characterStart;

    @Column(name = "character_end", nullable = false)
    private Integer characterEnd;

    @Column(name = "token_estimate")
    private Integer tokenEstimate;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeChunk() {
    }

    public KnowledgeChunk(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            UUID documentId,
            Integer chunkIndex,
            String content,
            String contentHash,
            Integer characterStart,
            Integer characterEnd,
            Integer tokenEstimate,
            String metadata,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.contentHash = contentHash;
        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.tokenEstimate = tokenEstimate;
        this.metadata = metadata;
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

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Integer getCharacterStart() {
        return characterStart;
    }

    public Integer getCharacterEnd() {
        return characterEnd;
    }

    public Integer getTokenEstimate() {
        return tokenEstimate;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
