package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.repository.ToolDefinitionRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ToolControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String ORG_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SEEDED_TOOL_ID = "77777777-7777-7777-7777-777777777701";
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ToolDefinitionRepository toolRepository;

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
    void listsSeededToolsAndExecutors() throws Exception {
        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/tools")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", "CURRENT_DATETIME")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].toolKey").value("CURRENT_DATETIME"));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/tools/executors")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executorKeys").isArray())
                .andExpect(jsonPath("$.executorKeys[?(@ == 'CALCULATOR')]").exists());
    }

    @Test
    void createUpdateActivateAndArchiveTool() throws Exception {
        String toolKey = "CUSTOM_TOOL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "_");
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/tools")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolKey":"%s",
                                  "name":"Custom tool",
                                  "description":"Test tool",
                                  "executorKey":"TEXT_STATISTICS",
                                  "inputSchema":"{\\"type\\":\\"object\\",\\"properties\\":{\\"text\\":{\\"type\\":\\"string\\",\\"maxLength\\":100}},\\"required\\":[\\"text\\"],\\"additionalProperties\\":false}",
                                  "requiresApproval":false,
                                  "maxExecutionSeconds":5,
                                  "maxOutputCharacters":5000
                                }
                                """.formatted(toolKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.toolType").value("BUILT_IN"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String toolId = created.get("id").asText();
        int version = created.get("version").asInt();

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/tools/" + toolId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":%d,
                                  "name":"Custom tool updated",
                                  "executorKey":"TEXT_STATISTICS",
                                  "inputSchema":"{\\"type\\":\\"object\\",\\"properties\\":{\\"text\\":{\\"type\\":\\"string\\",\\"maxLength\\":200}},\\"required\\":[\\"text\\"],\\"additionalProperties\\":false}",
                                  "requiresApproval":false,
                                  "maxExecutionSeconds":5,
                                  "maxOutputCharacters":5000
                                }
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Custom tool updated"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/tools/" + toolId + "/activate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/projects/" + PROJECT_ID + "/tools/" + toolId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(toolRepository.findById(UUID.fromString(toolId)))
                .isPresent()
                .get()
                .extracting(tool -> tool.getStatus())
                .isEqualTo(ToolStatus.ARCHIVED);
    }

    @Test
    void hidesCrossTenantToolsWithNotFound() throws Exception {
        String foreignOrgToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "foreign@nova.local",
                "Foreign",
                List.of("ORG_ADMIN"),
                List.of(),
                true));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/tools/" + SEEDED_TOOL_ID)
                        .header("Authorization", "Bearer " + foreignOrgToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void deniesCreateWithoutPermission() throws Exception {
        String readOnlyToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString(ORG_ID),
                "member@nova.local",
                "Member",
                List.of("ORG_MEMBER"),
                List.of("TOOL_READ"),
                true));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/tools")
                        .header("Authorization", "Bearer " + readOnlyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolKey":"NO_PERM_TOOL",
                                  "name":"Denied",
                                  "executorKey":"CALCULATOR",
                                  "inputSchema":"{\\"type\\":\\"object\\",\\"properties\\":{\\"operation\\":{\\"type\\":\\"string\\",\\"enum\\":[\\"ADD\\"]},\\"left\\":{\\"type\\":\\"number\\"},\\"right\\":{\\"type\\":\\"number\\"}},\\"required\\":[\\"operation\\",\\"left\\",\\"right\\"],\\"additionalProperties\\":false}",
                                  "requiresApproval":false
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
