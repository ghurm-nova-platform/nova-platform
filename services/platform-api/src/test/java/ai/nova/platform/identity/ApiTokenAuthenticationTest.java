package ai.nova.platform.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.service.ApiTokenService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTokenAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiTokenService apiTokenService;

    @Test
    @Transactional
    void apiTokenAuthenticatesProtectedEndpoint() throws Exception {
        var created = apiTokenService.createToken(
                IdentityTestFixture.ORG_ID, IdentityTestFixture.IDENTITY_USER_ID, "ci-token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + created.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@nova.local"));
    }
}
