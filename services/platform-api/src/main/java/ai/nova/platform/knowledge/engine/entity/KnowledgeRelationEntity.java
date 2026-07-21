package ai.nova.platform.knowledge.engine.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_engine_relations")
public class KnowledgeRelationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(name = "target_document_id")
    private UUID targetDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 40)
    private RelationType relationType;

    @Column(name = "target_ref_id")
    private UUID targetRefId;

    @Column(name = "target_ref_type", length = 80)
    private String targetRefType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected KnowledgeRelationEntity() {
    }

    public KnowledgeRelationEntity(
            UUID id,
            UUID organizationId,
            UUID sourceDocumentId,
            UUID targetDocumentId,
            RelationType relationType,
            UUID targetRefId,
            String targetRefType,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.sourceDocumentId = sourceDocumentId;
        this.targetDocumentId = targetDocumentId;
        this.relationType = relationType;
        this.targetRefId = targetRefId;
        this.targetRefType = targetRefType;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }

    public UUID getTargetDocumentId() {
        return targetDocumentId;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public UUID getTargetRefId() {
        return targetRefId;
    }

    public String getTargetRefType() {
        return targetRefType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
