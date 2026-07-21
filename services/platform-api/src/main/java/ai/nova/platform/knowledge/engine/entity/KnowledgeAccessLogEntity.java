package ai.nova.platform.knowledge.engine.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_engine_access_logs")
public class KnowledgeAccessLogEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeAccessLogEntity() {
    }

    public KnowledgeAccessLogEntity(
            UUID id,
            UUID documentId,
            UUID organizationId,
            UUID userId,
            String action,
            Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.organizationId = organizationId;
        this.userId = userId;
        this.action = action;
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

    public UUID getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
