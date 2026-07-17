package ai.nova.platform.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ORGANIZATION_ID = "organizationId";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        Assert.hasText(properties.getSecret(), "nova.security.jwt.secret (JWT_SECRET) must be configured");
        Assert.isTrue(
                properties.getSecret().getBytes(StandardCharsets.UTF_8).length >= 32,
                "JWT_SECRET must be at least 32 bytes");
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());

        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(user.getUserId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_USER_ID, user.getUserId().toString())
                .claim(CLAIM_ORGANIZATION_ID, user.getOrganizationId().toString())
                .claim(CLAIM_ROLES, user.getRoles())
                .claim(CLAIM_PERMISSIONS, user.getPermissions())
                .signWith(secretKey)
                .compact();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
        UUID organizationId = UUID.fromString(claims.get(CLAIM_ORGANIZATION_ID, String.class));
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get(CLAIM_PERMISSIONS, List.class);

        String subject = claims.getSubject();
        return new AuthenticatedUser(
                userId,
                organizationId,
                subject,
                subject,
                roles == null ? List.of() : List.copyOf(roles),
                permissions == null ? List.of() : List.copyOf(permissions),
                true);
    }

    public Duration getAccessTokenTtl() {
        return properties.getAccessTokenTtl();
    }

    public Duration getRefreshTokenTtl() {
        return properties.getRefreshTokenTtl();
    }
}
