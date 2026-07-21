package ai.nova.platform.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.identity.support.IdentityTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class ScimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String scimToken;

    @BeforeEach
    void setUp() {
        scimToken = jwtService.createAccessToken(IdentityTestFixture.scimProvisioner());
    }

    @Test
    void createAndListScimUser() throws Exception {
        mockMvc.perform(post("/api/scim/v2/Users")
                        .header("Authorization", "Bearer " + scimToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"scim-user@nova.local","displayName":"SCIM User","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("scim-user@nova.local"));

        mockMvc.perform(get("/api/scim/v2/Users").header("Authorization", "Bearer " + scimToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
