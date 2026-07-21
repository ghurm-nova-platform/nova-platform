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

    private static final int MAX_FAILED_LOGINS = 5;
    private static final java.time.Duration LOCK_DURATION = java.time.Duration.ofMinutes(15);

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

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "password_expires_at")
    private Instant passwordExpiresAt;

    @Column(name = "password_reset_token_hash", length = 128)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_token_expires_at")
    private Instant passwordResetTokenExpiresAt;

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
        this.failedLoginCount = 0;
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

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public Instant getPasswordExpiresAt() {
        return passwordExpiresAt;
    }

    public String getPasswordResetTokenHash() {
        return passwordResetTokenHash;
    }

    public Instant getPasswordResetTokenExpiresAt() {
        return passwordResetTokenExpiresAt;
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

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isPasswordExpired(Instant now) {
        return passwordExpiresAt != null && !passwordExpiresAt.isAfter(now);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public void setPasswordExpiresAt(Instant passwordExpiresAt) {
        this.passwordExpiresAt = passwordExpiresAt;
    }

    public void setPasswordResetToken(String hash, Instant expiresAt) {
        this.passwordResetTokenHash = hash;
        this.passwordResetTokenExpiresAt = expiresAt;
    }

    public void clearPasswordResetToken() {
        this.passwordResetTokenHash = null;
        this.passwordResetTokenExpiresAt = null;
    }

    public void unlock(Instant now) {
        this.lockedUntil = null;
        this.failedLoginCount = 0;
        this.updatedAt = now;
    }

    public void recordFailedLogin(Instant now) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= MAX_FAILED_LOGINS) {
            this.lockedUntil = now.plus(LOCK_DURATION);
        }
        this.updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
