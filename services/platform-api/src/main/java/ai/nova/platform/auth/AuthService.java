package ai.nova.platform.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse login(String email, String password) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
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
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenHash(hashToken(refreshTokenValue)).ifPresent(token -> {
            token.revoke(Instant.now());
            refreshTokenRepository.save(token);
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
                roleCodes(user));
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
                user.isEnabled());
    }

    private List<String> roleCodes(UserAccount user) {
        return user.getRoles().stream().map(Role::getCode).sorted().toList();
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
