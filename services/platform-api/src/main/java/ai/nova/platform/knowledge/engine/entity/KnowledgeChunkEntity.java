package ai.nova.platform.knowledge.engine.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_engine_chunks")
public class KnowledgeChunkEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "chunk_number", nullable = false)
    private int chunkNumber;

    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeChunkEntity() {
    }

    public KnowledgeChunkEntity(
            UUID id,
            UUID documentId,
            UUID organizationId,
            int chunkNumber,
            int startOffset,
            int endOffset,
            String content,
            Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.organizationId = organizationId;
        this.chunkNumber = chunkNumber;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
