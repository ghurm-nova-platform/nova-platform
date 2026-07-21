package ai.nova.platform.knowledge.engine.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_engine_attachments")
public class KnowledgeAttachmentEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "storage_ref", nullable = false, length = 1000)
    private String storageRef;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeAttachmentEntity() {
    }

    public KnowledgeAttachmentEntity(
            UUID id,
            UUID documentId,
            UUID organizationId,
            String fileName,
            String contentType,
            String storageRef,
            long sizeBytes,
            Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.organizationId = organizationId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.storageRef = storageRef;
        this.sizeBytes = sizeBytes;
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

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStorageRef() {
        return storageRef;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
