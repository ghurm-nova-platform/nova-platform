package ai.nova.platform.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.auth.RefreshToken;
import ai.nova.platform.auth.RefreshTokenRepository;
import ai.nova.platform.identity.entity.IdentityRefreshTokenEntity;
import ai.nova.platform.identity.entity.IdentitySessionEntity;
import ai.nova.platform.identity.repository.IdentityRefreshTokenRepository;
import ai.nova.platform.security.JwtService;
import ai.nova.platform.user.UserAccount;

@Service
public class RefreshTokenService {

    private final IdentityRefreshTokenRepository identityRefreshTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            IdentityRefreshTokenRepository identityRefreshTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService) {
        this.identityRefreshTokenRepository = identityRefreshTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String issueRefreshTokens(
            IdentitySessionEntity session, UUID identityUserId, UserAccount platformUser) {
        String refreshTokenValue = generateRefreshToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtService.getRefreshTokenTtl());
        String hash = hashToken(refreshTokenValue);

        identityRefreshTokenRepository.save(new IdentityRefreshTokenEntity(
                UUID.randomUUID(), session.getId(), identityUserId, hash, expiresAt, now));

        if (platformUser != null) {
            refreshTokenRepository.save(new RefreshToken(
                    UUID.randomUUID(), platformUser, hash, expiresAt, now));
        }
        return refreshTokenValue;
    }

    @Transactional
    public IdentityRefreshTokenEntity requireActiveIdentityRefreshToken(String refreshTokenValue) {
        return identityRefreshTokenRepository.findByTokenHash(hashToken(refreshTokenValue))
                .filter(token -> token.isActive(Instant.now()))
                .orElse(null);
    }

    @Transactional
    public void revokeRefreshToken(String refreshTokenValue) {
        Instant now = Instant.now();
        String hash = hashToken(refreshTokenValue);
        identityRefreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.revoke(now);
            identityRefreshTokenRepository.save(token);
        });
        revokeLegacyRefreshToken(refreshTokenValue, now);
    }

    @Transactional
    public void revokeLegacyRefreshToken(String refreshTokenValue, Instant now) {
        refreshTokenRepository.findByTokenHash(hashToken(refreshTokenValue)).ifPresent(token -> {
            token.revoke(now);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void rotateRefreshToken(String oldRefreshTokenValue, IdentitySessionEntity session, UserAccount user) {
        revokeRefreshToken(oldRefreshTokenValue);
        issueRefreshTokens(session, session.getIdentityUserId(), user);
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
