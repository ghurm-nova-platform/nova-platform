package ai.nova.platform.modelgateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class ModelGatewaySecurityTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void crossTenantProviderLookupReturns404() throws Exception {
        String token = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("22222222-2222-2222-2222-222222222299"),
                "other@nova.local",
                "Other",
                List.of("ORG_ADMIN"),
                List.of("MODEL_PROVIDER_READ"),
                true));
        mockMvc.perform(get("/api/model-providers/99999999-9999-9999-9999-999999999901")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingPermissionReturnsForbidden() throws Exception {
        String token = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "limited@nova.local",
                "Limited",
                List.of("USER"),
                List.of(),
                true));
        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/model-usage")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
