package ai.nova.platform.identity.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ai.nova.platform.identity.entity.ProviderType;

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
            boolean mfaEnabled) {
    }

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password,
            String mfaCode) {
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
            boolean defaultProvider) {
    }

    public record CreateProviderRequest(
            @NotBlank String name,
            @NotNull ProviderType providerType,
            String configJson) {
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

    public record UserSummaryView(UUID id, String email, String displayName, boolean mfaEnabled) {
    }

    public record GroupSummaryView(UUID id, String name) {
    }

    public record SummaryView(
            List<UserSummaryView> users,
            List<GroupSummaryView> groups,
            List<ProviderView> providers) {
    }

    public record ApiTokenCreateResponse(UUID id, String token, String tokenPrefix) {
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
