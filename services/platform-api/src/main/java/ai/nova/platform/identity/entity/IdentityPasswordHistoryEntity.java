package ai.nova.platform.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_password_history")
public class IdentityPasswordHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "identity_user_id", nullable = false)
    private UUID identityUserId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentityPasswordHistoryEntity() {
    }

    public IdentityPasswordHistoryEntity(UUID id, UUID identityUserId, String passwordHash, Instant createdAt) {
        this.id = id;
        this.identityUserId = identityUserId;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdentityUserId() {
        return identityUserId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
