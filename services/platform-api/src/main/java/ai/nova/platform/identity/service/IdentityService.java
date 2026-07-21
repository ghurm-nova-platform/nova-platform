package ai.nova.platform.identity.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.configuration.IdentityProperties;
import ai.nova.platform.identity.dto.IdentityDtos.ConfigResponse;
import ai.nova.platform.identity.dto.IdentityDtos.DashboardView;
import ai.nova.platform.identity.dto.IdentityDtos.LoginHistoryView;
import ai.nova.platform.identity.dto.IdentityDtos.SecurityEventView;
import ai.nova.platform.identity.dto.IdentityDtos.SessionView;
import ai.nova.platform.identity.dto.IdentityDtos.SummaryView;
import ai.nova.platform.identity.dto.IdentityDtos.UserSummaryView;
import ai.nova.platform.identity.entity.LoginResult;
import ai.nova.platform.identity.mapper.IdentityEntityMapper;
import ai.nova.platform.identity.repository.IdentityGroupRepository;
import ai.nova.platform.identity.repository.IdentityLoginHistoryRepository;
import ai.nova.platform.identity.repository.IdentityProviderRepository;
import ai.nova.platform.identity.repository.IdentityUserRepository;

@Service
public class IdentityService {

    private final IdentityProperties properties;
    private final IdentityProviderService providerService;
    private final SessionService sessionService;
    private final IdentityUserRepository identityUserRepository;
    private final IdentityGroupRepository identityGroupRepository;
    private final IdentityLoginHistoryRepository loginHistoryRepository;
    private final IdentityProviderRepository providerRepository;

    public IdentityService(
            IdentityProperties properties,
            IdentityProviderService providerService,
            SessionService sessionService,
            IdentityUserRepository identityUserRepository,
            IdentityGroupRepository identityGroupRepository,
            IdentityLoginHistoryRepository loginHistoryRepository,
            IdentityProviderRepository providerRepository) {
        this.properties = properties;
        this.providerService = providerService;
        this.sessionService = sessionService;
        this.identityUserRepository = identityUserRepository;
        this.identityGroupRepository = identityGroupRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public ConfigResponse getConfig() {
        return new ConfigResponse(
                properties.isEnabled(),
                properties.getSessionIdleTimeout().toString(),
                properties.getSessionAbsoluteTimeout().toString(),
                properties.getMaxConcurrentSessions(),
                properties.getMfa().isEnabled(),
                properties.getJwt().isEnabled(),
                properties.getLdap().isEnabled(),
                properties.getOidc().isEnabled(),
                properties.getSaml().isEnabled(),
                properties.getScim().isEnabled());
    }

    @Transactional(readOnly = true)
    public List<SessionView> listSessions(UUID organizationId) {
        return sessionService.listActiveSessions(organizationId);
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryView> loginHistory(UUID organizationId) {
        return loginHistoryRepository.findTop50ByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(IdentityEntityMapper::toLoginHistoryView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SecurityEventView> securityEvents(UUID organizationId) {
        return loginHistoryRepository.findTop50ByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(IdentityEntityMapper::toSecurityEventView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard(UUID organizationId) {
        Instant now = Instant.now();
        Instant since = now.minus(24, ChronoUnit.HOURS);
        long totalUsers = identityUserRepository.countByOrganizationId(organizationId);
        long mfaUsers = identityUserRepository.countByOrganizationIdAndMfaEnabledTrue(organizationId);
        double mfaAdoption = totalUsers == 0 ? 0.0 : (double) mfaUsers / totalUsers;
        List<LoginHistoryView> recentLogins =
                loginHistoryRepository.findTop20ByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                        .map(IdentityEntityMapper::toLoginHistoryView)
                        .toList();
        return new DashboardView(
                identityUserRepository.countByOrganizationIdAndEnabledTrue(organizationId),
                sessionService.countActiveSessions(organizationId),
                loginHistoryRepository.countByOrganizationIdAndResultAndCreatedAtAfter(
                        organizationId, LoginResult.FAILURE, since),
                identityUserRepository.countByOrganizationIdAndLockedUntilAfter(organizationId, now),
                mfaAdoption,
                providerRepository.findByOrganizationIdOrderByNameAsc(organizationId).size(),
                recentLogins);
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
