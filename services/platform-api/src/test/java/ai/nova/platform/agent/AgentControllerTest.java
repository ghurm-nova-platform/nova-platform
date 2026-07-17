package ai.nova.platform.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentRepository agentRepository;

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
    void createListUpdateActivateAndArchiveAgent() throws Exception {
        String uniqueName = "Reviewer-" + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "description":"Helps with reviews",
                                  "systemPrompt":"Be careful and concise.",
                                  "modelProvider":"OPENAI",
                                  "modelName":"gpt-4.1-mini",
                                  "temperature":0.2,
                                  "maxTokens":1024,
                                  "visibility":"PROJECT"
                                }
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(uniqueName))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.organizationId").value("11111111-1111-1111-1111-111111111111"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String agentId = created.get("id").asText();
        int version = created.get("version").asInt();

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/agents")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", uniqueName)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(uniqueName));

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/agents/" + agentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "description":"Updated",
                                  "systemPrompt":"Updated prompt",
                                  "modelProvider":"ANTHROPIC",
                                  "modelName":"claude-sonnet",
                                  "temperature":0.4,
                                  "maxTokens":2048,
                                  "visibility":"PRIVATE",
                                  "version":%d
                                }
                                """.formatted(uniqueName, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelProvider").value("ANTHROPIC"))
                .andExpect(jsonPath("$.version").value(version + 1));

        mockMvc.perform(patch("/api/projects/" + PROJECT_ID + "/agents/" + agentId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE","version":%d}
                                """.formatted(version + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/projects/" + PROJECT_ID + "/agents/" + agentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        Agent archived = agentRepository.findById(UUID.fromString(agentId)).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(AgentStatus.ARCHIVED);
    }

    @Test
    void rejectsUnknownProviderAndDuplicateNames() throws Exception {
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Bad Provider",
                                  "systemPrompt":"x",
                                  "modelProvider":"UNKNOWN_VENDOR",
                                  "modelName":"x",
                                  "temperature":0.1,
                                  "visibility":"PROJECT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MODEL_PROVIDER"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Demo Code Reviewer",
                                  "systemPrompt":"x",
                                  "modelProvider":"OPENAI",
                                  "modelName":"gpt-4.1-mini",
                                  "temperature":0.1,
                                  "visibility":"PROJECT"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AGENT_NAME_EXISTS"));
    }

    @Test
    void hidesCrossTenantAgentsWithNotFound() throws Exception {
        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/agents/77777777-7777-7777-7777-777777777777")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENT_NOT_FOUND"));
    }

    @Test
    void detectsOptimisticLockConflicts() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Lock-%s",
                                  "systemPrompt":"prompt",
                                  "modelProvider":"LOCAL",
                                  "modelName":"local-model",
                                  "temperature":0.0,
                                  "visibility":"PRIVATE"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String agentId = created.get("id").asText();

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/agents/" + agentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "systemPrompt":"prompt",
                                  "modelProvider":"LOCAL",
                                  "modelName":"local-model",
                                  "temperature":0.0,
                                  "visibility":"PRIVATE",
                                  "version":999
                                }
                                """.formatted(created.get("name").asText())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }
}
