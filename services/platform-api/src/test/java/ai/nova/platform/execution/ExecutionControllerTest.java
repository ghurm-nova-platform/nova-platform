package ai.nova.platform.execution;

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
class ExecutionControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String ORG_ID = "11111111-1111-1111-1111-111111111111";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

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
    void executeDemoAgentWithVariables() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"I need help with my order"},
                                  "variables":{"customer_name":"Alex","topic":"billing"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.executionId").isNotEmpty())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("Deterministic runtime response")))
                .andExpect(jsonPath("$.model.providerId").value("99999999-9999-9999-9999-999999999901"))
                .andExpect(jsonPath("$.model.modelId").value("99999999-9999-9999-9999-999999999911"))
                .andExpect(jsonPath("$.latencyMs").isNumber())
                .andExpect(jsonPath("$.tokens.input").isNumber())
                .andExpect(jsonPath("$.tokens.output").isNumber())
                .andExpect(jsonPath("$.tokens.total").isNumber())
                .andExpect(jsonPath("$.renderedPrompt").value("Hello Alex, thank you for contacting support about billing."))
                .andReturn();

        String executionId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("executionId")
                .asText();

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/executions/" + executionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(executionId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.messages.length()").value(3));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/executions")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("agentId", DEMO_AGENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agentId").value(DEMO_AGENT_ID));
    }

    @Test
    void rejectsInactiveAgent() throws Exception {
        String uniqueName = "Draft-" + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "systemPrompt":"Draft agent",
                                  "modelProvider":"OPENAI",
                                  "modelName":"gpt-4.1-mini",
                                  "temperature":0.2,
                                  "visibility":"PROJECT"
                                }
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andReturn();
        String draftAgentId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + draftAgentId + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"input":{"message":"hello"}}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AGENT_NOT_ACTIVE"));
    }

    @Test
    void rejectsMissingRequiredVariables() throws Exception {
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"hello"},
                                  "variables":{"customer_name":"Alex"}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_VARIABLE"));
    }

    @Test
    void cancelCompletedExecutionReturnsConflict() throws Exception {
        MvcResult executeResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"cancel test"},
                                  "variables":{"customer_name":"Sam","topic":"returns"}
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String executionId = objectMapper
                .readTree(executeResult.getResponse().getContentAsString())
                .get("executionId")
                .asText();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/executions/" + executionId + "/cancel")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXECUTION_CANCELLED"));
    }

    @Test
    void hidesCrossTenantExecutionsWithNotFound() throws Exception {
        String foreignOrgToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "foreign@nova.local",
                "Foreign",
                List.of("ORG_ADMIN"),
                List.of(),
                true));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/executions")
                        .header("Authorization", "Bearer " + foreignOrgToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/executions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXECUTION_NOT_FOUND"));
    }

    @Test
    void rejectsUnauthorizedExecutePermission() throws Exception {
        String limitedToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString(ORG_ID),
                "limited@nova.local",
                "Limited",
                List.of("USER"),
                List.of("AGENT_READ"),
                true));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + limitedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"hello"},
                                  "variables":{"customer_name":"Alex","topic":"billing"}
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
