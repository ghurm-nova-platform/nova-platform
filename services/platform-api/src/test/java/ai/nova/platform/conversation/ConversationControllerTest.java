package ai.nova.platform.conversation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ConversationControllerTest {

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
    void createListGetUpdateArchiveRestoreAndMessages() throws Exception {
        String title = "Support chat " + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentId":"%s","title":"%s"}
                                """.formatted(DEMO_AGENT_ID, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.messageCount").value(0))
                .andExpect(jsonPath("$.agentId").value(DEMO_AGENT_ID))
                .andReturn();

        String conversationId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("agentId", DEMO_AGENT_ID)
                        .param("status", "ACTIVE")
                        .param("search", title.substring(0, 8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(conversationId));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId));

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Renamed chat","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed chat"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Hello from user"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.sequenceNumber").value(1));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Hello from user"));

        mockMvc.perform(delete("/api/projects/" + PROJECT_ID + "/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Should fail"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONVERSATION_ARCHIVED"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/conversations/" + conversationId + "/restore")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void hidesCrossTenantConversationsWithNotFound() throws Exception {
        String foreignOrgToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "foreign@nova.local",
                "Foreign",
                List.of("ORG_ADMIN"),
                List.of(),
                true));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/conversations")
                        .header("Authorization", "Bearer " + foreignOrgToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/conversations/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }

    @Test
    void rejectsAgentMismatchOnCreate() throws Exception {
        String uniqueName = "Other-" + UUID.randomUUID();
        MvcResult createAgentResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "systemPrompt":"Other agent",
                                  "modelProvider":"OPENAI",
                                  "modelName":"gpt-4.1-mini",
                                  "temperature":0.2,
                                  "visibility":"PROJECT"
                                }
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode otherAgent = objectMapper.readTree(createAgentResult.getResponse().getContentAsString());
        String otherAgentId = otherAgent.get("id").asText();
        int otherAgentVersion = otherAgent.get("version").asInt();

        mockMvc.perform(patch("/api/projects/" + PROJECT_ID + "/agents/" + otherAgentId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE","version":%d}
                                """.formatted(otherAgentVersion)))
                .andExpect(status().isOk());

        MvcResult convResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentId":"%s","title":"Mismatch test"}
                                """.formatted(DEMO_AGENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String conversationId = objectMapper
                .readTree(convResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + otherAgentId + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"hello"},
                                  "variables":{"customer_name":"Alex","topic":"billing"},
                                  "conversationId":"%s",
                                  "clientRequestId":"%s"
                                }
                                """.formatted(conversationId, UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONVERSATION_AGENT_MISMATCH"));
    }
}
