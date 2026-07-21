package ai.nova.platform.identity.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.auth.AuthenticationFailedException;
import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.identity.dto.IdentityDtos.GenericMessageResponse;
import ai.nova.platform.identity.dto.IdentityDtos.ValidateTokenResponse;
import ai.nova.platform.identity.entity.IdentityLoginHistoryEntity;
import ai.nova.platform.identity.entity.IdentityProviderEntity;
import ai.nova.platform.identity.entity.IdentityRefreshTokenEntity;
import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.entity.IdentityUserEntity;
import ai.nova.platform.identity.entity.LoginResult;
import ai.nova.platform.identity.entity.ProviderType;
import ai.nova.platform.identity.error.IdentityErrorCodes;
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

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int RESET_TOKEN_HOURS = 1;

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
    private final IdentityUserService identityUserService;
    private final PasswordPolicyService passwordPolicyService;
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
            IdentityUserService identityUserService,
            PasswordPolicyService passwordPolicyService,
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
        this.identityUserService = identityUserService;
        this.passwordPolicyService = passwordPolicyService;
        this.auditProperties = auditProperties;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional
    public TokenResponse login(String email, String password, String mfaCode, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        UserAccount platformUser = userAccountRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        UUID organizationId = platformUser != null ? platformUser.getOrganization().getId() : null;

        IdentityProviderEntity provider = organizationId != null
                ? identityProviderService.resolveLocalProvider(organizationId)
                : null;

        IdentityUserEntity identityUser = organizationId != null
                ? identityUserRepository
                        .findByOrganizationIdAndEmailIgnoreCase(organizationId, email.trim())
                        .orElse(null)
                : null;

        if (identityUser != null) {
            if (!identityUser.isEnabled()) {
                recordLoginFailure(organizationId, identityUser.getId(), provider, ipAddress, userAgent, "Account disabled");
                throw new ApiException(HttpStatus.FORBIDDEN, IdentityErrorCodes.ACCOUNT_DISABLED, "Account is disabled");
            }
            if (identityUser.isLocked(now)) {
                recordLoginFailure(organizationId, identityUser.getId(), provider, ipAddress, userAgent, "Account locked");
                throw new ApiException(HttpStatus.FORBIDDEN, IdentityErrorCodes.ACCOUNT_LOCKED, "Account is locked");
            }
        }

        AuthenticationResult authResult = providerRegistry
                .resolve(ProviderType.LOCAL)
                .authenticate(new AuthenticationCredentials(email, password, null));

        if (!authResult.success()) {
            if (identityUser != null) {
                identityUser.recordFailedLogin(now);
                identityUserRepository.save(identityUser);
            }
            recordLoginFailure(organizationId, identityUser != null ? identityUser.getId() : null, provider, ipAddress, userAgent, authResult.failureReason());
            throw new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.INVALID_CREDENTIALS, "Invalid email or password");
        }

        platformUser = authResult.platformUser();
        organizationId = platformUser.getOrganization().getId();
        provider = identityProviderService.resolveLocalProvider(organizationId);
        identityUser = resolveIdentityUser(platformUser, provider);

        if (!identityUser.isEnabled() || !platformUser.isEnabled()) {
            recordLoginFailure(organizationId, identityUser.getId(), provider, ipAddress, userAgent, "Account disabled");
            throw new ApiException(HttpStatus.FORBIDDEN, IdentityErrorCodes.ACCOUNT_DISABLED, "Account is disabled");
        }
        if (identityUser.isLocked(now)) {
            recordLoginFailure(organizationId, identityUser.getId(), provider, ipAddress, userAgent, "Account locked");
            throw new ApiException(HttpStatus.FORBIDDEN, IdentityErrorCodes.ACCOUNT_LOCKED, "Account is locked");
        }
        if (identityUser.isPasswordExpired(now) || identityUser.isForcePasswordChange()) {
            recordLogin(organizationId, identityUser.getId(), provider.getId(), LoginResult.FAILURE, ipAddress, userAgent, "Password expired");
            throw new ApiException(HttpStatus.FORBIDDEN, IdentityErrorCodes.PASSWORD_EXPIRED, "Password change required");
        }

        if (mfaService.isMfaRequired(identityUser.getId())) {
            if (mfaCode == null || mfaCode.isBlank()) {
                recordLogin(organizationId, identityUser.getId(), provider.getId(), LoginResult.MFA_REQUIRED, ipAddress, userAgent, "MFA code required");
                throw new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.MFA_REQUIRED, "Multi-factor authentication code required");
            }
            if (!mfaService.verify(identityUser.getId(), mfaCode)) {
                identityUser.recordFailedLogin(now);
                identityUserRepository.save(identityUser);
                recordLogin(organizationId, identityUser.getId(), provider.getId(), LoginResult.FAILURE, ipAddress, userAgent, "Invalid MFA code");
                throw new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.INVALID_CREDENTIALS, "Invalid MFA code");
            }
        }

        identityUser.recordSuccessfulLogin(now);
        identityUserRepository.save(identityUser);

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
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.TOKEN_INVALID, "Invalid refresh token"));

        Instant now = Instant.now();
        if (!stored.isActive(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.TOKEN_EXPIRED, "Refresh token is expired or revoked");
        }

        stored.revoke(now);
        identityRefreshTokenRepository.save(stored);
        refreshTokenService.revokeLegacyRefreshToken(refreshTokenValue, now);

        IdentityUserEntity identityUser = identityUserRepository.findById(stored.getIdentityUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.TOKEN_INVALID, "User not found"));
        UserAccount platformUser = identityUser.getPlatformUserId() != null
                ? userAccountRepository.findById(identityUser.getPlatformUserId())
                        .filter(UserAccount::isEnabled)
                        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.ACCOUNT_DISABLED, "User account is disabled"))
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

    @Transactional
    public void logoutAll(UUID identityUserId, String ipAddress, String userAgent) {
        sessionService.revokeAllSessionsForUser(identityUserId);
        identityUserRepository.findById(identityUserId).ifPresent(identityUser -> {
            if (identityUser.getPlatformUserId() != null) {
                userAccountRepository.findById(identityUser.getPlatformUserId()).ifPresent(user -> {
                    if (auditProperties.isEnabled() && auditProperties.isCaptureSecurityEvents()) {
                        publishSecurityAudit(user, null, AuditAction.LOGOUT, AuditResult.SUCCESS, ipAddress, userAgent);
                    }
                });
            }
        });
    }

    @Transactional(readOnly = true)
    public ValidateTokenResponse validateToken(String accessToken) {
        try {
            AuthenticatedUser user = identityJwtService.parseAccessToken(accessToken);
            return new ValidateTokenResponse(
                    true, user.getUserId(), user.getOrganizationId(), user.getEmail(), user.getRoles());
        } catch (Exception ex) {
            return new ValidateTokenResponse(false, null, null, null, List.of());
        }
    }

    @Transactional
    public void changePassword(UUID identityUserId, String currentPassword, String newPassword) {
        IdentityUserEntity identityUser = identityUserRepository
                .findById(identityUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_USER_NOT_FOUND, "Identity user not found"));
        if (identityUser.getPlatformUserId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_NOT_SUPPORTED", "Password change not supported");
        }
        UserAccount platformUser = userAccountRepository
                .findById(identityUser.getPlatformUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, IdentityErrorCodes.IDENTITY_USER_NOT_FOUND, "Platform user not found"));
        AuthenticationResult authResult = providerRegistry
                .resolve(ProviderType.LOCAL)
                .authenticate(new AuthenticationCredentials(platformUser.getEmail(), currentPassword, null));
        if (!authResult.success()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, IdentityErrorCodes.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        identityUserService.applyPasswordChange(identityUser, newPassword, false);
    }

    @Transactional
    public GenericMessageResponse forgotPassword(String email) {
        identityUserRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            String token = refreshTokenService.generateRefreshToken();
            Instant expires = Instant.now().plus(RESET_TOKEN_HOURS, ChronoUnit.HOURS);
            user.setPasswordResetToken(passwordPolicyService.hashValue(token), expires);
            user.touch(Instant.now());
            identityUserRepository.save(user);
            log.info("Password reset token issued for {} (token logged for dev): {}", email, token);
        });
        return new GenericMessageResponse("If the account exists, a password reset link has been sent.");
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hash = passwordPolicyService.hashValue(token);
        Instant now = Instant.now();
        IdentityUserEntity user = identityUserRepository.findAll().stream()
                .filter(u -> hash.equals(u.getPasswordResetTokenHash()))
                .filter(u -> u.getPasswordResetTokenExpiresAt() != null && u.getPasswordResetTokenExpiresAt().isAfter(now))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, IdentityErrorCodes.TOKEN_INVALID, "Invalid or expired reset token"));
        identityUserService.applyPasswordChange(user, newPassword, false);
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
