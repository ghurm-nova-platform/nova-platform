package ai.nova.platform.modelgateway;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ModelGatewayControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_PROVIDER_ID = "99999999-9999-9999-9999-999999999901";
    private static final String DEMO_MODEL_ID = "99999999-9999-9999-9999-999999999911";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";

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
    void listsAdaptersAndSeededProvider() throws Exception {
        mockMvc.perform(get("/api/model-providers/adapters")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adapters[?(@.adapterKey == 'DETERMINISTIC_LOCAL')]").exists());

        mockMvc.perform(get("/api/model-providers")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", "NOVA_LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DEMO_PROVIDER_ID));
    }

    @Test
    void listsProjectModelsAndAgentAssignments() throws Exception {
        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/models")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelId").value(DEMO_MODEL_ID));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/models")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelId").value(DEMO_MODEL_ID));
    }

    @Test
    void listsRoutingPolicyAndUsage() throws Exception {
        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/model-routing-policies")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].strategy").value("PRIORITY_FALLBACK"));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/model-usage")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray());
    }
}
