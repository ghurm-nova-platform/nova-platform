package ai.nova.platform.identity.entity;

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
@Table(name = "identity_mfa_factors")
public class IdentityMfaFactorEntity {

    @Id
    private UUID id;

    @Column(name = "identity_user_id", nullable = false)
    private UUID identityUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "factor_type", nullable = false, length = 20)
    private MfaFactorType factorType;

    @Column(name = "secret_encrypted", nullable = false, length = 500)
    private String secretEncrypted;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentityMfaFactorEntity() {
    }

    public IdentityMfaFactorEntity(
            UUID id,
            UUID identityUserId,
            MfaFactorType factorType,
            String secretEncrypted,
            Instant createdAt) {
        this.id = id;
        this.identityUserId = identityUserId;
        this.factorType = factorType;
        this.secretEncrypted = secretEncrypted;
        this.enabled = false;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdentityUserId() {
        return identityUserId;
    }

    public MfaFactorType getFactorType() {
        return factorType;
    }

    public String getSecretEncrypted() {
        return secretEncrypted;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void activate(Instant at) {
        this.enabled = true;
        this.enrolledAt = at;
    }
}
