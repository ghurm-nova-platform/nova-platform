package ai.nova.platform.audit.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_indexes")
public class AuditIndexEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "audit_event_id", nullable = false)
    private UUID auditEventId;

    @Column(name = "index_key", nullable = false, length = 60)
    private String indexKey;

    @Column(name = "index_value", nullable = false, length = 500)
    private String indexValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditIndexEntity() {
    }

    public AuditIndexEntity(
            UUID id,
            UUID organizationId,
            UUID auditEventId,
            String indexKey,
            String indexValue,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.auditEventId = auditEventId;
        this.indexKey = indexKey;
        this.indexValue = indexValue;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getAuditEventId() {
        return auditEventId;
    }

    public String getIndexKey() {
        return indexKey;
    }

    public String getIndexValue() {
        return indexValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
