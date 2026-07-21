package ai.nova.platform.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.identity.support.IdentityTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtProperties;
import ai.nova.platform.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
class IdentitySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    private String adminToken;
    private String readToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.createAccessToken(IdentityTestFixture.identityAdminUser());
        readToken = jwtService.createAccessToken(IdentityTestFixture.identityReadUser());
    }

    @Test
    void readOnlyUserForbiddenOnUserCreate() throws Exception {
        mockMvc.perform(post("/api/identity/users")
                        .header("Authorization", "Bearer " + readToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new-user@nova.local","displayName":"New User","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanGetIdentityConfig() throws Exception {
        mockMvc.perform(get("/api/identity/config").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void crossTenantUserLookupNotFound() throws Exception {
        AuthenticatedUser otherTenant = new AuthenticatedUser(
                IdentityTestFixture.USER_ID,
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(),
                true);
        String otherOrgToken = jwtService.createAccessToken(otherTenant);

        mockMvc.perform(get("/api/identity/users/" + IdentityTestFixture.IDENTITY_USER_ID)
                        .header("Authorization", "Bearer " + otherOrgToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void tamperedJwtRejected() throws Exception {
        mockMvc.perform(get("/api/identity/config").header("Authorization", "Bearer " + adminToken + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwtRejected() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(IdentityTestFixture.USER_ID.toString())
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .claim(JwtService.CLAIM_USER_ID, IdentityTestFixture.USER_ID.toString())
                .claim(JwtService.CLAIM_ORGANIZATION_ID, IdentityTestFixture.ORG_ID.toString())
                .claim(JwtService.CLAIM_ROLES, List.of("ORG_ADMIN"))
                .claim(JwtService.CLAIM_PERMISSIONS, List.of())
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/identity/config").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }
}
