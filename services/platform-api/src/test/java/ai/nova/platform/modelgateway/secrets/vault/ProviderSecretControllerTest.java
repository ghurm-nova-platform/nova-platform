package ai.nova.platform.modelgateway.secrets.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class ProviderSecretControllerTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void login() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@nova.local","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    @Test
    void createReturnsMetadataOnlyAndGetNeverReturnsSecret() throws Exception {
        String plaintext = "super-secret-value-xyz9";
        MvcResult createResult = mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"OPENAI_KEY_%s",
                                  "name":"OpenAI Key",
                                  "providerType":"OPENAI",
                                  "secret":"%s"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(), plaintext)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.credentialReference").value(org.hamcrest.Matchers.startsWith("vault:provider-secret:")))
                .andExpect(jsonPath("$.last4").value("xyz9"))
                .andExpect(jsonPath("$.fingerprintSha256").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.ciphertext").doesNotExist())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String body = createResult.getResponse().getContentAsString();
        assertThat(body).doesNotContain(plaintext);

        String secretId = created.get("id").asText();
        MvcResult getResult = mockMvc.perform(get("/api/provider-secrets/" + secretId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andReturn();
        assertThat(getResult.getResponse().getContentAsString()).doesNotContain(plaintext);
    }

    @Test
    void rotateCreatesNewSecretAndMarksPreviousRotated() throws Exception {
        String keySuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        MvcResult createResult = mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"ROTATE_%s",
                                  "name":"Rotate Key",
                                  "providerType":"OPENAI",
                                  "secret":"initial-secret-aaaa"
                                }
                                """.formatted(keySuffix)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String previousId = created.get("id").asText();
        String previousKey = created.get("secretKey").asText();

        MvcResult rotateResult = mockMvc.perform(post("/api/provider-secrets/" + previousId + "/rotate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"secret":"rotated-secret-bbbb"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(previousId)))
                .andExpect(jsonPath("$.secretKey").value(previousKey))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.last4").value("bbbb"))
                .andExpect(jsonPath("$.fingerprintSha256").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andReturn();
        String replacementId = objectMapper.readTree(rotateResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/provider-secrets/" + previousId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ROTATED"))
                .andExpect(jsonPath("$.rotatedAt").isNotEmpty())
                .andExpect(jsonPath("$.secretKey").value(org.hamcrest.Matchers.startsWith(previousKey + "__ROTATED_")));

        mockMvc.perform(post("/api/provider-secrets/" + replacementId + "/revoke")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokedAt").isNotEmpty());
    }

    @Test
    void crossTenantSecretReturns404() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"CROSS_%s",
                                  "name":"Cross",
                                  "providerType":"OPENAI",
                                  "secret":"tenant-secret-cccc"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())))
                .andExpect(status().isCreated())
                .andReturn();
        String secretId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String otherToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("22222222-2222-2222-2222-222222222299"),
                "other@nova.local",
                "Other",
                List.of("ORG_ADMIN"),
                List.of("PROVIDER_SECRET_READ"),
                true));

        mockMvc.perform(get("/api/provider-secrets/" + secretId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rbacMatrixForSecretPermissions() throws Exception {
        String projectAdminToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "pa@nova.local",
                "Project Admin",
                List.of("PROJECT_ADMIN"),
                List.of("PROVIDER_SECRET_READ", "PROVIDER_CONNECTION_TEST"),
                true));

        mockMvc.perform(get("/api/provider-secrets")
                        .header("Authorization", "Bearer " + projectAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/provider-secrets")
                        .header("Authorization", "Bearer " + projectAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretKey":"FORBIDDEN_CREATE",
                                  "name":"Nope",
                                  "providerType":"OPENAI",
                                  "secret":"should-fail-dddd"
                                }
                                """))
                .andExpect(status().isForbidden());

        String userToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "user@nova.local",
                "User",
                List.of("USER"),
                List.of(),
                true));
        mockMvc.perform(get("/api/provider-secrets")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
