package ai.nova.platform.knowledge.entity;

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
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "knowledge_base_id", nullable = false)
    private UUID knowledgeBaseId;

    @Column(name = "document_key", nullable = false, length = 100)
    private String documentKey;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private KnowledgeDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KnowledgeDocumentStatus status;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "extracted_character_count")
    private Integer extractedCharacterCount;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount;

    @Column(name = "ingestion_error_code", length = 100)
    private String ingestionErrorCode;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    protected KnowledgeDocument() {
    }

    public KnowledgeDocument(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            String documentKey,
            String fileName,
            String mediaType,
            KnowledgeDocumentType documentType,
            KnowledgeDocumentStatus status,
            String contentHash,
            Long fileSizeBytes,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.documentKey = documentKey;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.documentType = documentType;
        this.status = status;
        this.contentHash = contentHash;
        this.fileSizeBytes = fileSizeBytes;
        this.chunkCount = 0;
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

    public UUID getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public String getDocumentKey() {
        return documentKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public KnowledgeDocumentType getDocumentType() {
        return documentType;
    }

    public KnowledgeDocumentStatus getStatus() {
        return status;
    }

    public void setStatus(KnowledgeDocumentStatus status) {
        this.status = status;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Integer getExtractedCharacterCount() {
        return extractedCharacterCount;
    }

    public void setExtractedCharacterCount(Integer extractedCharacterCount) {
        this.extractedCharacterCount = extractedCharacterCount;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getIngestionErrorCode() {
        return ingestionErrorCode;
    }

    public void setIngestionErrorCode(String ingestionErrorCode) {
        this.ingestionErrorCode = ingestionErrorCode;
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

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
