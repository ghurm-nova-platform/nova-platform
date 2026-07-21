package ai.nova.platform.identity.mapper;

import java.time.Instant;

import ai.nova.platform.identity.dto.IdentityDtos.ApiTokenView;
import ai.nova.platform.identity.dto.IdentityDtos.GroupSummaryView;
import ai.nova.platform.identity.dto.IdentityDtos.GroupView;
import ai.nova.platform.identity.dto.IdentityDtos.LoginHistoryView;
import ai.nova.platform.identity.dto.IdentityDtos.PermissionView;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderView;
import ai.nova.platform.identity.dto.IdentityDtos.RoleView;
import ai.nova.platform.identity.dto.IdentityDtos.SecurityEventView;
import ai.nova.platform.identity.dto.IdentityDtos.ServiceAccountView;
import ai.nova.platform.identity.dto.IdentityDtos.SessionView;
import ai.nova.platform.identity.dto.IdentityDtos.UserSummaryView;
import ai.nova.platform.identity.dto.IdentityDtos.UserView;
import ai.nova.platform.identity.entity.IdentityApiTokenEntity;
import ai.nova.platform.identity.entity.IdentityGroupEntity;
import ai.nova.platform.identity.entity.IdentityLoginHistoryEntity;
import ai.nova.platform.identity.entity.IdentityPermissionEntity;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.IdentityRoleEntity;
import ai.nova.platform.identity.entity.IdentityServiceAccountEntity;
import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.entity.IdentityUserEntity;

public final class IdentityEntityMapper {

    private IdentityEntityMapper() {
    }

    public static ProviderView toProviderView(IdentityProviderEntity entity) {
        return new ProviderView(
                entity.getId(),
                entity.getName(),
                entity.getProviderType(),
                entity.getStatus().name(),
                entity.isDefaultProvider(),
                entity.getConfigJson());
    }

    public static SessionView toSessionView(IdentitySessionEntity entity) {
        return new SessionView(
                entity.getId(),
                entity.getIdentityUserId(),
                entity.getStatus().name(),
                entity.getIpAddress(),
                entity.getCreatedAt(),
                entity.getLastAccessedAt(),
                entity.getExpiresAt());
    }

    public static LoginHistoryView toLoginHistoryView(IdentityLoginHistoryEntity entity) {
        return new LoginHistoryView(
                entity.getId(),
                entity.getIdentityUserId(),
                entity.getResult().name(),
                entity.getIpAddress(),
                entity.getCreatedAt(),
                entity.getFailureReason());
    }

    public static SecurityEventView toSecurityEventView(IdentityLoginHistoryEntity entity) {
        return new SecurityEventView(
                entity.getId(),
                "LOGIN",
                entity.getIdentityUserId(),
                entity.getResult().name(),
                entity.getIpAddress(),
                entity.getCreatedAt(),
                entity.getFailureReason());
    }

    public static UserSummaryView toUserSummaryView(IdentityUserEntity entity) {
        return new UserSummaryView(
                entity.getId(), entity.getEmail(), entity.getDisplayName(), entity.isMfaEnabled());
    }

    public static UserView toUserView(IdentityUserEntity entity, Instant now) {
        return new UserView(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.isEnabled(),
                entity.isMfaEnabled(),
                entity.isLocked(now),
                entity.getFailedLoginCount(),
                entity.getLockedUntil(),
                entity.getPasswordExpiresAt(),
                entity.getProviderId(),
                entity.getPlatformUserId(),
                entity.getCreatedAt());
    }

    public static GroupSummaryView toGroupSummaryView(IdentityGroupEntity entity) {
        return new GroupSummaryView(entity.getId(), entity.getName());
    }

    public static GroupView toGroupView(IdentityGroupEntity entity) {
        return new GroupView(
                entity.getId(), entity.getName(), entity.getExternalId(), entity.getDescription(), entity.getCreatedAt());
    }

    public static RoleView toRoleView(IdentityRoleEntity entity) {
        return new RoleView(
                entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(), entity.getCreatedAt());
    }

    public static PermissionView toPermissionView(IdentityPermissionEntity entity) {
        return new PermissionView(
                entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(), entity.getCreatedAt());
    }

    public static ApiTokenView toApiTokenView(IdentityApiTokenEntity entity) {
        return new ApiTokenView(
                entity.getId(),
                entity.getName(),
                entity.getTokenPrefix(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getLastUsedAt(),
                entity.getCreatedAt());
    }

    public static ServiceAccountView toServiceAccountView(IdentityServiceAccountEntity entity) {
        return new ServiceAccountView(
                entity.getId(), entity.getName(), entity.getClientId(), entity.isEnabled(), entity.getCreatedAt());
    }
}
