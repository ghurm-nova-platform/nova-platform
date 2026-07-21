package ai.nova.platform.identity.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.identity.dto.IdentityDtos.ConfigResponse;
import ai.nova.platform.identity.dto.IdentityDtos.LoginHistoryView;
import ai.nova.platform.identity.dto.IdentityDtos.SessionView;
import ai.nova.platform.identity.dto.IdentityDtos.SummaryView;
import ai.nova.platform.identity.dto.IdentityDtos.UserSummaryView;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityGroupRepository;
import ai.nova.platform.identity.repository.IdentityLoginHistoryRepository;
import ai.nova.platform.identity.repository.IdentityUserRepository;

@Service
public class IdentityService {

    private final IdentityProperties properties;
    private final IdentityProviderService providerService;
    private final SessionService sessionService;
    private final IdentityUserRepository identityUserRepository;
    private final IdentityGroupRepository identityGroupRepository;
    private final IdentityLoginHistoryRepository loginHistoryRepository;

    public IdentityService(
            IdentityProperties properties,
            IdentityProviderService providerService,
            SessionService sessionService,
            IdentityUserRepository identityUserRepository,
            IdentityGroupRepository identityGroupRepository,
            IdentityLoginHistoryRepository loginHistoryRepository) {
        this.properties = properties;
        this.providerService = providerService;
        this.sessionService = sessionService;
        this.identityUserRepository = identityUserRepository;
        this.identityGroupRepository = identityGroupRepository;
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Transactional(readOnly = true)
    public ConfigResponse getConfig() {
        return new ConfigResponse(
                properties.isEnabled(),
                properties.getSessionIdleTimeout().toString(),
                properties.getSessionAbsoluteTimeout().toString(),
                properties.getMaxConcurrentSessions(),
                properties.getMfa().isEnabled());
    }

    @Transactional(readOnly = true)
    public List<SessionView> listSessions(UUID organizationId) {
        return sessionService.listActiveSessions(organizationId).stream()
                .map(IdentityEntityMapper::toSessionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryView> loginHistory(UUID organizationId) {
        return loginHistoryRepository.findTop50ByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(IdentityEntityMapper::toLoginHistoryView)
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryView summary(UUID organizationId) {
        List<UserSummaryView> users = identityUserRepository.findByOrganizationIdOrderByEmailAsc(organizationId).stream()
                .map(IdentityEntityMapper::toUserSummaryView)
                .toList();
        var groups = identityGroupRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(IdentityEntityMapper::toGroupSummaryView)
                .toList();
        var providers = providerService.listProviders(organizationId);
        return new SummaryView(users, groups, providers);
    }
}
