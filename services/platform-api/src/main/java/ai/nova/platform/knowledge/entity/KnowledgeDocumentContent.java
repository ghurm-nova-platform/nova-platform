package ai.nova.platform.knowledge.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_document_content")
public class KnowledgeDocumentContent {

    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "knowledge_base_id", nullable = false)
    private UUID knowledgeBaseId;

    @Column(name = "extracted_text", nullable = false, columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeDocumentContent() {
    }

    public KnowledgeDocumentContent(
            UUID documentId,
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            String extractedText,
            Instant createdAt) {
        this.documentId = documentId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.extractedText = extractedText;
        this.createdAt = createdAt;
    }

    public UUID getDocumentId() {
        return documentId;
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

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
