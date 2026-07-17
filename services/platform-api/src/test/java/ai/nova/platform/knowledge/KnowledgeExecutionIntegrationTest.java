package ai.nova.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeExecutionIntegrationTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String AGENT_ID = "66666666-6666-6666-6666-666666666601";
    private static final String DEMO_KB_ID = "88888888-8888-8888-8888-888888888801";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void login() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@nova.local","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    @Test
    void executeIncludesKnowledgeCitationMarkers() throws Exception {
        String content = "Shipping SLA for Nova is two business days for standard parcels.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "shipping.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(
                                "/api/projects/" + PROJECT_ID + "/knowledge-bases/" + DEMO_KB_ID + "/documents")
                        .file(file)
                        .param("documentKey", "SHIP_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY"));

        MvcResult executeResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"What is the shipping SLA for parcels?"},
                                  "variables":{"customer_name":"Ada","topic":"shipping"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        JsonNode response = objectMapper.readTree(executeResult.getResponse().getContentAsString());
        String text = response.get("response").asText();
        assertThat(text).contains("knowledgeCitations=");
        assertThat(response.get("citations").isArray()).isTrue();
        if (response.get("citations").size() > 0) {
            assertThat(response.get("citations").get(0).has("content")).isFalse();
            assertThat(response.get("citations").get(0).has("label")).isTrue();
        }
    }
}
