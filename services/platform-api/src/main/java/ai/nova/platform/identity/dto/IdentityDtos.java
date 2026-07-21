package ai.nova.platform.identity.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ai.nova.platform.identity.entity.ProviderType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class IdentityDtos {

    private IdentityDtos() {
    }

    public record ConfigResponse(
            boolean enabled,
            String sessionIdleTimeout,
            String sessionAbsoluteTimeout,
            int maxConcurrentSessions,
            boolean mfaEnabled,
            boolean jwtEnabled,
            boolean ldapEnabled,
            boolean oidcEnabled,
            boolean samlEnabled,
            boolean scimEnabled) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            String mfaCode) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record ValidateTokenRequest(@NotBlank String accessToken) {
    }

    public record ValidateTokenResponse(
            boolean valid,
            UUID userId,
            UUID organizationId,
            String email,
            List<String> roles) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank String newPassword) {
    }

    public record AdminResetPasswordRequest(@NotBlank String newPassword) {
    }

    public record GenericMessageResponse(String message) {
    }

    public record MfaVerifyRequest(@NotBlank String mfaCode) {
    }

    public record MfaEnrollResponse(String secret, String otpAuthUri, List<String> recoveryCodes) {
    }

    public record ProviderView(
            UUID id,
            String name,
            ProviderType providerType,
            String status,
            boolean defaultProvider,
            String configJson) {
    }

    public record CreateProviderRequest(
            @NotBlank String name,
            @NotNull ProviderType providerType,
            String configJson) {
    }

    public record UpdateProviderRequest(
            String name,
            String status,
            String configJson,
            Boolean defaultProvider) {
    }

    public record ProviderTestResponse(boolean success, String message) {
    }

    public record SessionView(
            UUID id,
            UUID identityUserId,
            String status,
            String ipAddress,
            Instant createdAt,
            Instant lastAccessedAt,
            Instant expiresAt) {
    }

    public record LoginHistoryView(
            UUID id,
            UUID identityUserId,
            String result,
            String ipAddress,
            Instant createdAt,
            String failureReason) {
    }

    public record SecurityEventView(
            UUID id,
            String eventType,
            UUID identityUserId,
            String result,
            String ipAddress,
            Instant occurredAt,
            String detail) {
    }

    public record UserView(
            UUID id,
            String email,
            String displayName,
            boolean enabled,
            boolean mfaEnabled,
            boolean locked,
            int failedLoginCount,
            Instant lockedUntil,
            Instant passwordExpiresAt,
            UUID providerId,
            UUID platformUserId,
            Instant createdAt) {
    }

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank String displayName,
            UUID providerId,
            String password) {
    }

    public record UpdateUserRequest(
            String displayName,
            Boolean enabled,
            UUID providerId) {
    }

    public record UserSummaryView(UUID id, String email, String displayName, boolean mfaEnabled) {
    }

    public record GroupView(UUID id, String name, String externalId, String description, Instant createdAt) {
    }

    public record CreateGroupRequest(
            @NotBlank String name,
            String externalId,
            String description) {
    }

    public record UpdateGroupRequest(String name, String externalId, String description) {
    }

    public record GroupSummaryView(UUID id, String name) {
    }

    public record RoleView(UUID id, String code, String name, String description, Instant createdAt) {
    }

    public record CreateRoleRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description) {
    }

    public record UpdateRoleRequest(String name, String description) {
    }

    public record PermissionView(UUID id, String code, String name, String description, Instant createdAt) {
    }

    public record CreatePermissionRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description) {
    }

    public record UpdatePermissionRequest(String name, String description) {
    }

    public record ApiTokenView(
            UUID id,
            String name,
            String tokenPrefix,
            Instant expiresAt,
            Instant revokedAt,
            Instant lastUsedAt,
            Instant createdAt) {
    }

    public record CreateApiTokenRequest(@NotBlank String name) {
    }

    public record ApiTokenCreateResponse(UUID id, String token, String tokenPrefix) {
    }

    public record ServiceAccountView(
            UUID id,
            String name,
            String clientId,
            boolean enabled,
            Instant createdAt) {
    }

    public record CreateServiceAccountRequest(@NotBlank String name) {
    }

    public record UpdateServiceAccountRequest(String name, Boolean enabled) {
    }

    public record ServiceAccountCreateResponse(
            UUID id,
            String name,
            String clientId,
            String clientSecret) {
    }

    public record DashboardView(
            long activeUsers,
            long onlineSessions,
            long failedLogins,
            long lockedAccounts,
            double mfaAdoption,
            long providerCount,
            List<LoginHistoryView> recentLogins) {
    }

    public record SummaryView(
            List<UserSummaryView> users,
            List<GroupSummaryView> groups,
            List<ProviderView> providers) {
    }

    public record ExportRequest(String format) {
    }

    public record ScimUserRequest(
            @NotBlank String userName,
            String displayName,
            boolean active) {
    }

    public record ScimUserResponse(
            UUID id,
            String userName,
            String displayName,
            boolean active) {
    }

    public record ScimListResponse<T>(List<T> Resources, int totalResults) {
    }
}
