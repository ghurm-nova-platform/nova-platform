package ai.nova.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-secret-key-32bytes-minimum!!";
    private static final String ISSUER = "nova-platform-test";

    private JwtService jwtService;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer(ISSUER);
        jwtService = new JwtService(properties);
    }

    @Test
    void accessTokenContainsRequiredClaims() {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444401");
        UUID organizationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                organizationId,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("AGENT_READ", "AGENT_CREATE"),
                true);

        String token = jwtService.createAccessToken(user);
        AuthenticatedUser parsed = jwtService.parseAccessToken(token);

        assertThat(parsed.getUserId()).isEqualTo(userId);
        assertThat(parsed.getOrganizationId()).isEqualTo(organizationId);
        assertThat(parsed.getRoles()).containsExactly("ORG_ADMIN");
        assertThat(parsed.getPermissions()).contains("AGENT_READ", "AGENT_CREATE");
        assertThat(parsed.getAuthorities()).extracting(Object::toString).contains("ROLE_ORG_ADMIN", "AGENT_READ");
    }

    @Test
    void rejectsTamperedToken() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                UUID.randomUUID(),
                "user@nova.local",
                "User",
                List.of("ORG_MEMBER"),
                List.of("AGENT_READ"),
                true);
        String token = jwtService.createAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseAccessToken(token + "x"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Instant past = Instant.now().minus(Duration.ofHours(1));
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String expired = Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .claim(JwtService.CLAIM_USER_ID, userId.toString())
                .claim(JwtService.CLAIM_ORGANIZATION_ID, organizationId.toString())
                .claim(JwtService.CLAIM_ROLES, List.of("ORG_MEMBER"))
                .claim(JwtService.CLAIM_PERMISSIONS, List.of("AGENT_READ"))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAccessToken(expired)).isInstanceOf(RuntimeException.class);
    }
}
