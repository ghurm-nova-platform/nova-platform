package ai.nova.platform.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.auth.AuthDtos.MeResponse;
import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.audit.service.AuditStorageService;
import ai.nova.platform.permission.Permission;
import ai.nova.platform.role.Role;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;
import ai.nova.platform.user.UserAccount;
import ai.nova.platform.user.UserAccountRepository;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditProperties auditProperties;
    private final AuditRecordingSupport auditRecordingSupport;
    private final AuditStorageService auditStorageService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditProperties auditProperties,
            AuditRecordingSupport auditRecordingSupport,
            AuditStorageService auditStorageService) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditProperties = auditProperties;
        this.auditRecordingSupport = auditRecordingSupport;
        this.auditStorageService = auditStorageService;
    }

    @Transactional
    public TokenResponse login(String email, String password, String ipAddress, String userAgent) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> {
                    publishLoginFailure(null, email, ipAddress, userAgent);
                    return new AuthenticationFailedException("Invalid email or password");
                });

        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            publishLoginFailure(user, email, ipAddress, userAgent);
            throw new AuthenticationFailedException("Invalid email or password");
        }

        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        if (auditProperties.isEnabled() && auditProperties.isCaptureSecurityEvents()) {
            auditStorageService.startSession(
                    sessionId,
                    user.getId(),
                    user.getOrganization().getId(),
                    ipAddress,
                    userAgent,
                    now);
            auditRecordingSupport.recordSecurityEvent(
                    user.getOrganization().getId(),
                    user.getId(),
                    user.getDisplayName(),
                    sessionId,
                    AuditAction.LOGIN,
                    AuditResult.SUCCESS,
                    ipAddress,
                    userAgent);
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(refreshTokenValue))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));

        Instant now = Instant.now();
        if (!stored.isActive(now)) {
            throw new AuthenticationFailedException("Refresh token is expired or revoked");
        }

        stored.revoke(now);
        refreshTokenRepository.save(stored);

        UserAccount user = stored.getUser();
        if (!user.isEnabled()) {
            throw new AuthenticationFailedException("User account is disabled");
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshTokenValue, String ipAddress, String userAgent) {
        refreshTokenRepository.findByTokenHash(hashToken(refreshTokenValue)).ifPresent(token -> {
            token.revoke(Instant.now());
            refreshTokenRepository.save(token);
            UserAccount user = token.getUser();
            if (auditProperties.isEnabled() && auditProperties.isCaptureSecurityEvents()) {
                auditRecordingSupport.recordSecurityEvent(
                        user.getOrganization().getId(),
                        user.getId(),
                        user.getDisplayName(),
                        null,
                        AuditAction.LOGOUT,
                        AuditResult.SUCCESS,
                        ipAddress,
                        userAgent);
            }
        });
    }

    @Transactional(readOnly = true)
    public MeResponse me(AuthenticatedUser principal) {
        UserAccount user = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        return new MeResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmail(),
                user.getDisplayName(),
                roleCodes(user),
                permissionCodes(user));
    }

    private void publishLoginFailure(UserAccount user, String email, String ipAddress, String userAgent) {
        if (!auditProperties.isEnabled() || !auditProperties.isCaptureSecurityEvents()) {
            return;
        }
        UUID organizationId = user != null ? user.getOrganization().getId() : null;
        UUID userId = user != null ? user.getId() : null;
        String username = user != null ? user.getDisplayName() : email;
        if (organizationId == null) {
            return;
        }
        auditRecordingSupport.recordSecurityEvent(
                organizationId,
                userId,
                username,
                null,
                AuditAction.LOGIN,
                AuditResult.FAILURE,
                ipAddress,
                userAgent);
    }

    private TokenResponse issueTokens(UserAccount user) {
        AuthenticatedUser principal = toPrincipal(user);
        String accessToken = jwtService.createAccessToken(principal);

        String refreshTokenValue = generateRefreshToken();
        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                hashToken(refreshTokenValue),
                now.plus(jwtService.getRefreshTokenTtl()),
                now);
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessTokenTtl().toSeconds());
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

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
