package ai.nova.platform.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.auth.AuthenticationFailedException;
import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.identity.entity.IdentityLoginHistoryEntity;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.IdentityRefreshTokenEntity;
import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.entity.LoginResult;
import ai.nova.platform.identity.entity.ProviderStatus;
import ai.nova.platform.identity.entity.ProviderType;
import ai.nova.platform.identity.provider.AuthenticationCredentials;
import ai.nova.platform.identity.provider.AuthenticationResult;
import ai.nova.platform.identity.provider.IdentityProviderRegistry;
import ai.nova.platform.identity.repository.IdentityLoginHistoryRepository;
import ai.nova.platform.identity.repository.IdentityRefreshTokenRepository;
import ai.nova.platform.identity.repository.IdentityUserRepository;
import ai.nova.platform.permission.Permission;
import ai.nova.platform.role.Role;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class AuthenticationService {

    private final IdentityProviderService identityProviderService;
    private final IdentityProviderRegistry providerRegistry;
    private final IdentityUserRepository identityUserRepository;
    private final UserAccountRepository userAccountRepository;
    private final IdentityLoginHistoryRepository loginHistoryRepository;
    private final IdentityRefreshTokenRepository identityRefreshTokenRepository;
    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;
    private final IdentityJwtService identityJwtService;
    private final MfaService mfaService;
    private final AuditProperties auditProperties;
    private final AuditRecordingSupport auditRecordingSupport;

    public AuthenticationService(
            IdentityProviderService identityProviderService,
            IdentityProviderRegistry providerRegistry,
            IdentityUserRepository identityUserRepository,
            UserAccountRepository userAccountRepository,
            IdentityLoginHistoryRepository loginHistoryRepository,
            IdentityRefreshTokenRepository identityRefreshTokenRepository,
            SessionService sessionService,
            RefreshTokenService refreshTokenService,
            IdentityJwtService identityJwtService,
            MfaService mfaService,
            AuditProperties auditProperties,
            AuditRecordingSupport auditRecordingSupport) {
        this.identityProviderService = identityProviderService;
        this.providerRegistry = providerRegistry;
        this.identityUserRepository = identityUserRepository;
        this.userAccountRepository = userAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.identityRefreshTokenRepository = identityRefreshTokenRepository;
        this.sessionService = sessionService;
        this.refreshTokenService = refreshTokenService;
        this.identityJwtService = identityJwtService;
        this.mfaService = mfaService;
        this.auditProperties = auditProperties;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional
    public TokenResponse login(String email, String password, String mfaCode, String ipAddress, String userAgent) {
        UserAccount platformUser = userAccountRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        UUID organizationId = platformUser != null ? platformUser.getOrganization().getId() : null;

        IdentityProviderEntity provider = organizationId != null
                ? identityProviderService.resolveLocalProvider(organizationId)
                : null;

        AuthenticationResult authResult = providerRegistry
                .resolve(ProviderType.LOCAL)
                .authenticate(new AuthenticationCredentials(email, password, null));

        if (!authResult.success()) {
            recordLoginFailure(organizationId, null, provider, ipAddress, userAgent, authResult.failureReason());
            throw new AuthenticationFailedException("Invalid email or password");
        }

        platformUser = authResult.platformUser();
        organizationId = platformUser.getOrganization().getId();
        provider = identityProviderService.resolveLocalProvider(organizationId);
        IdentityUserEntity identityUser = resolveIdentityUser(platformUser, provider);

        if (mfaService.isMfaRequired(identityUser.getId())) {
            if (mfaCode == null || mfaCode.isBlank()) {
                recordLogin(organizationId, identityUser.getId(), provider.getId(), LoginResult.MFA_REQUIRED, ipAddress, userAgent, "MFA code required");
                throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_REQUIRED", "Multi-factor authentication code required");
            }
            if (!mfaService.verify(identityUser.getId(), mfaCode)) {
                recordLogin(organizationId, identityUser.getId(), provider.getId(), LoginResult.FAILURE, ipAddress, userAgent, "Invalid MFA code");
                throw new AuthenticationFailedException("Invalid MFA code");
            }
        }

        IdentitySessionEntity session = sessionService.createSession(
                organizationId, identityUser.getId(), platformUser.getId(), ipAddress, userAgent);
        recordLogin(organizationId, identityUser.getId(), provider.getId(), LoginResult.SUCCESS, ipAddress, userAgent, null);
        publishSecurityAudit(platformUser, session.getId(), AuditAction.LOGIN, AuditResult.SUCCESS, ipAddress, userAgent);
        return issueTokens(platformUser, identityUser, session);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        String hash = refreshTokenService.hashToken(refreshTokenValue);
        IdentityRefreshTokenEntity stored = identityRefreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));

        Instant now = Instant.now();
        if (!stored.isActive(now)) {
            throw new AuthenticationFailedException("Refresh token is expired or revoked");
        }

        stored.revoke(now);
        identityRefreshTokenRepository.save(stored);
        refreshTokenService.revokeLegacyRefreshToken(refreshTokenValue, now);

        IdentityUserEntity identityUser = identityUserRepository.findById(stored.getIdentityUserId())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));
        UserAccount platformUser = identityUser.getPlatformUserId() != null
                ? userAccountRepository.findById(identityUser.getPlatformUserId())
                        .filter(UserAccount::isEnabled)
                        .orElseThrow(() -> new AuthenticationFailedException("User account is disabled"))
                : null;

        IdentitySessionEntity session = sessionService.requireActiveSession(stored.getSessionId());
        sessionService.touchSession(session.getId());
        return issueTokens(platformUser, identityUser, session);
    }

    @Transactional
    public void logout(String refreshTokenValue, String ipAddress, String userAgent) {
        String hash = refreshTokenService.hashToken(refreshTokenValue);
        identityRefreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            sessionService.revokeSession(token.getSessionId());
            identityUserRepository.findById(token.getIdentityUserId()).ifPresent(identityUser -> {
                if (identityUser.getPlatformUserId() != null) {
                    userAccountRepository.findById(identityUser.getPlatformUserId()).ifPresent(user -> {
                        if (auditProperties.isEnabled() && auditProperties.isCaptureSecurityEvents()) {
                            publishSecurityAudit(user, null, AuditAction.LOGOUT, AuditResult.SUCCESS, ipAddress, userAgent);
                        }
                    });
                }
            });
        });
        refreshTokenService.revokeRefreshToken(refreshTokenValue);
    }

    private TokenResponse issueTokens(UserAccount platformUser, IdentityUserEntity identityUser, IdentitySessionEntity session) {
        AuthenticatedUser principal = toPrincipal(platformUser);
        String accessToken = identityJwtService.createAccessToken(principal);
        String refreshTokenValue = refreshTokenService.issueRefreshTokens(session, identityUser.getId(), platformUser);
        return new TokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                identityJwtService.getAccessTokenTtl().toSeconds());
    }

    private IdentityUserEntity resolveIdentityUser(UserAccount platformUser, IdentityProviderEntity provider) {
        return identityUserRepository.findByPlatformUserId(platformUser.getId())
                .or(() -> identityUserRepository.findByOrganizationIdAndEmailIgnoreCase(
                        platformUser.getOrganization().getId(), platformUser.getEmail()))
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    IdentityUserEntity created = new IdentityUserEntity(
                            UUID.randomUUID(),
                            platformUser.getOrganization().getId(),
                            platformUser.getId(),
                            provider.getId(),
                            platformUser.getId().toString(),
                            platformUser.getEmail(),
                            platformUser.getDisplayName(),
                            now);
                    return identityUserRepository.save(created);
                });
    }

    private AuthenticatedUser toPrincipal(UserAccount user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmail(),
                user.getDisplayName(),
                roleCodes(user),
                permissionCodes(user),
                user.isEnabled());
    }

    private List<String> roleCodes(UserAccount user) {
        return user.getRoles().stream().map(Role::getCode).sorted().toList();
    }

    private List<String> permissionCodes(UserAccount user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
    }

    private void recordLogin(
            UUID organizationId,
            UUID identityUserId,
            UUID providerId,
            LoginResult result,
            String ipAddress,
            String userAgent,
            String failureReason) {
        if (organizationId == null) {
            return;
        }
        loginHistoryRepository.save(new IdentityLoginHistoryEntity(
                UUID.randomUUID(),
                organizationId,
                identityUserId,
                providerId,
                result,
                ipAddress,
                userAgent,
                failureReason,
                Instant.now()));
    }

    private void recordLoginFailure(
            UUID organizationId,
            UUID identityUserId,
            IdentityProviderEntity provider,
            String ipAddress,
            String userAgent,
            String failureReason) {
        recordLogin(
                organizationId,
                identityUserId,
                provider != null ? provider.getId() : null,
                LoginResult.FAILURE,
                ipAddress,
                userAgent,
                failureReason);
        if (organizationId != null && auditProperties.isEnabled() && auditProperties.isCaptureSecurityEvents()) {
            auditRecordingSupport.recordSecurityEvent(
                    organizationId,
                    null,
                    null,
                    null,
                    AuditAction.LOGIN,
                    AuditResult.FAILURE,
                    ipAddress,
                    userAgent);
        }
    }

    private void publishSecurityAudit(
            UserAccount user,
            UUID sessionId,
            AuditAction action,
            AuditResult result,
            String ipAddress,
            String userAgent) {
        if (auditProperties.isEnabled() && auditProperties.isCaptureSecurityEvents()) {
            auditRecordingSupport.recordSecurityEvent(
                    user.getOrganization().getId(),
                    user.getId(),
                    user.getDisplayName(),
                    sessionId,
                    action,
                    result,
                    ipAddress,
                    userAgent);
        }
    }
}
