package ai.nova.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-only-jwt-secret-key-32bytes-minimum!!");
        properties.setIssuer("nova-platform-test");
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
                true);

        String token = jwtService.createAccessToken(user);
        AuthenticatedUser parsed = jwtService.parseAccessToken(token);

        assertThat(parsed.getUserId()).isEqualTo(userId);
        assertThat(parsed.getOrganizationId()).isEqualTo(organizationId);
        assertThat(parsed.getRoles()).containsExactly("ORG_ADMIN");
        assertThat(parsed.getAuthorities()).extracting(Object::toString).contains("ROLE_ORG_ADMIN");
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
                true);
        String token = jwtService.createAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseAccessToken(token + "x"))
                .isInstanceOf(RuntimeException.class);
    }
}
