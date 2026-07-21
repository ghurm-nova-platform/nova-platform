package ai.nova.platform.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "identity_users")
public class IdentityUserEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "platform_user_id")
    private UUID platformUserId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "external_subject", length = 500)
    private String externalSubject;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityUserEntity() {
    }

    public IdentityUserEntity(
            UUID id,
            UUID organizationId,
            UUID platformUserId,
            UUID providerId,
            String externalSubject,
            String email,
            String displayName,
            Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.platformUserId = platformUserId;
        this.providerId = providerId;
        this.externalSubject = externalSubject;
        this.email = email;
        this.displayName = displayName;
        this.enabled = true;
        this.mfaEnabled = false;
        this.forcePasswordChange = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getExternalSubject() {
        return externalSubject;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }

    public void setPasswordChangedAt(Instant passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
