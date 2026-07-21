package ai.nova.platform.llm;

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

import ai.nova.platform.llm.support.LlmTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class LlmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.createAccessToken(LlmTestFixture.llmAdminUser());
    }

    @Test
    void configHealthAndChat() throws Exception {
        mockMvc.perform(get("/api/llm/config").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.defaultProvider").value("DETERMINISTIC"));

        mockMvc.perform(get("/api/llm/models").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists());

        mockMvc.perform(post("/api/llm/chat")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "modelCode":"deterministic-chat-v1",
                                  "messages":[{"role":"user","content":"controller hello"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Local LLM: controller hello"))
                .andExpect(jsonPath("$.providerType").value("DETERMINISTIC"));
    }

    @Test
    void requiresAuth() throws Exception {
        mockMvc.perform(get("/api/llm/config")).andExpect(status().isUnauthorized());
    }
}
