package ai.nova.platform.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_recovery_codes")
public class IdentityRecoveryCodeEntity {

    @Id
    private UUID id;

    @Column(name = "identity_user_id", nullable = false)
    private UUID identityUserId;

    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentityRecoveryCodeEntity() {
    }

    public IdentityRecoveryCodeEntity(UUID id, UUID identityUserId, String codeHash, Instant createdAt) {
        this.id = id;
        this.identityUserId = identityUserId;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdentityUserId() {
        return identityUserId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(Instant at) {
        this.usedAt = at;
    }
}
