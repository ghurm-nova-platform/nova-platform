package ai.nova.platform.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.identity.support.IdentityTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class IdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.createAccessToken(IdentityTestFixture.identityAdminUser());
    }

    @Test
    void configSessionsDashboardAndSummary() throws Exception {
        mockMvc.perform(get("/api/identity/config").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.mfaEnabled").value(true));

        mockMvc.perform(get("/api/identity/sessions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/identity/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.providers").isArray());

        mockMvc.perform(get("/api/identity/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUsers").isNumber())
                .andExpect(jsonPath("$.providerCount").isNumber());
    }

    @Test
    void usersAndProvidersEndpoints() throws Exception {
        mockMvc.perform(get("/api/identity/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@nova.local"));

        mockMvc.perform(get("/api/identity/providers").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Local Authentication"));
    }

    @Test
    void revokeSessionEndpoint() throws Exception {
        mockMvc.perform(delete("/api/identity/sessions/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
