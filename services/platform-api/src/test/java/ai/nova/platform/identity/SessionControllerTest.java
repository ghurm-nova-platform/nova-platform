package ai.nova.platform.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class SessionControllerTest {

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
    void listSessionsWithAdminToken() throws Exception {
        mockMvc.perform(get("/api/identity/sessions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
