package ai.nova.platform.identity.mapper;

import ai.nova.platform.identity.dto.IdentityDtos.GroupSummaryView;
import ai.nova.platform.identity.dto.IdentityDtos.LoginHistoryView;
import ai.nova.platform.identity.dto.IdentityDtos.ProviderView;
import ai.nova.platform.identity.dto.IdentityDtos.SessionView;
import ai.nova.platform.identity.dto.IdentityDtos.UserSummaryView;
import ai.nova.platform.identity.entity.IdentityGroupEntity;
import ai.nova.platform.identity.entity.IdentityLoginHistoryEntity;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
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
                entity.isDefaultProvider());
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

    public static UserSummaryView toUserSummaryView(IdentityUserEntity entity) {
        return new UserSummaryView(
                entity.getId(), entity.getEmail(), entity.getDisplayName(), entity.isMfaEnabled());
    }

    public static GroupSummaryView toGroupSummaryView(IdentityGroupEntity entity) {
        return new GroupSummaryView(entity.getId(), entity.getName());
    }
}
