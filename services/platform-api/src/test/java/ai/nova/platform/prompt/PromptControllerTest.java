package ai.nova.platform.prompt;

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

import ai.nova.platform.prompt.entity.PromptAuditAction;
import ai.nova.platform.prompt.entity.PromptAuditLog;
import ai.nova.platform.prompt.entity.PromptStatus;
import ai.nova.platform.prompt.entity.PromptVersionStatus;
import ai.nova.platform.prompt.repository.PromptAuditLogRepository;
import ai.nova.platform.prompt.repository.PromptRepository;
import ai.nova.platform.prompt.repository.PromptVersionRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class PromptControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String ORG_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private PromptVersionRepository promptVersionRepository;

    @Autowired
    private PromptAuditLogRepository promptAuditLogRepository;

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
    void createListUpdatePublishRollbackAndArchive() throws Exception {
        String uniqueName = "Prompt-" + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "description":"Support template",
                                  "promptType":"CHAT",
                                  "content":"Hello {{customer_name}} about {{topic}}",
                                  "changeSummary":"Initial draft",
                                  "tags":["support","chat"],
                                  "variables":[
                                    {"name":"customer_name","dataType":"STRING","required":true,"sampleValue":"Alex"},
                                    {"name":"topic","dataType":"STRING","required":true,"sampleValue":"billing"}
                                  ]
                                }
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(uniqueName))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentDraftVersionNumber").value(1))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String promptId = created.get("id").asText();
        String versionId = created.get("currentDraftVersionId").asText();
        int version = created.get("version").asInt();

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/prompts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", uniqueName)
                        .param("status", "DRAFT")
                        .param("tag", "support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(uniqueName));

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/prompts/" + promptId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "description":"Updated description",
                                  "promptType":"SYSTEM",
                                  "tags":["support"],
                                  "version":%d
                                }
                                """.formatted(uniqueName, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptType").value("SYSTEM"))
                .andExpect(jsonPath("$.version").value(version + 1));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + versionId + "/publish")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Ready for production"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeSummary":"Next draft"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        MvcResult versionsResult = mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode versions = objectMapper.readTree(versionsResult.getResponse().getContentAsString());
        String publishedVersionId = versions.get(1).get("id").asText();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/rollback")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceVersionId":"%s","reason":"Restore published copy"}
                                """.formatted(publishedVersionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_DRAFT_VERSION"));

        mockMvc.perform(delete("/api/projects/" + PROJECT_ID + "/prompts/" + promptId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(promptRepository.findById(UUID.fromString(promptId)).orElseThrow().getStatus())
                .isEqualTo(PromptStatus.ARCHIVED);
    }

    @Test
    void rejectsImmutablePublishedVersionUpdates() throws Exception {
        String uniqueName = "Immutable-" + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "promptType":"CHAT",
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String promptId = created.get("id").asText();
        String versionId = created.get("currentDraftVersionId").asText();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + versionId + "/publish")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + versionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Changed",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMPT_VERSION_IMMUTABLE"));
    }

    @Test
    void rejectsDuplicateNames() throws Exception {
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Demo Support Reply",
                                  "promptType":"CHAT",
                                  "content":"Static text"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMPT_NAME_EXISTS"));
    }

    @Test
    void hidesCrossTenantPromptsWithNotFound() throws Exception {
        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/prompts/77777777-7777-7777-7777-777777777777")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMPT_NOT_FOUND"));
    }

    @Test
    void validateAndPreviewEndpointsWork() throws Exception {
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/validate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.detectedVariables[0]").value("name"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/preview")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}],
                                  "values":{"name":"Sam"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renderedContent").value("Hello Sam"));
    }

    @Test
    void validateAndPreviewRequireProjectInOrganization() throws Exception {
        String missingProjectId = "99999999-9999-9999-9999-999999999999";
        mockMvc.perform(post("/api/projects/" + missingProjectId + "/prompts/validate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

        mockMvc.perform(post("/api/projects/" + missingProjectId + "/prompts/preview")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}],
                                  "values":{"name":"Sam"}
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void validateAndPreviewRejectCrossTenantOrganizationTokens() throws Exception {
        String foreignOrgToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "foreign@nova.local",
                "Foreign",
                List.of("ORG_ADMIN"),
                List.of(),
                true));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/validate")
                        .header("Authorization", "Bearer " + foreignOrgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/preview")
                        .header("Authorization", "Bearer " + foreignOrgToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}],
                                  "values":{"name":"Sam"}
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void validateAndPreviewRejectUnauthorizedPermissions() throws Exception {
        String noPromptPermsToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString(ORG_ID),
                "limited@nova.local",
                "Limited",
                List.of("USER"),
                List.of("AGENT_READ"),
                true));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/validate")
                        .header("Authorization", "Bearer " + noPromptPermsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/preview")
                        .header("Authorization", "Bearer " + noPromptPermsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}],
                                  "values":{"name":"Sam"}
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void versionUpdateAuditStoresMetadataNotRawContent() throws Exception {
        String uniqueName = "Audit-" + UUID.randomUUID();
        String secretContent = "SECRET_TOKEN_DO_NOT_AUDIT {{name}}";
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "promptType":"CHAT",
                                  "content":"%s",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """.formatted(uniqueName, secretContent)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String promptId = created.get("id").asText();
        String versionId = created.get("currentDraftVersionId").asText();
        String updatedContent = "UPDATED_SECRET_VALUE {{name}}";

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + versionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"%s",
                                  "changeSummary":"sanitize audit",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """.formatted(updatedContent)))
                .andExpect(status().isOk());

        List<PromptAuditLog> audits = promptAuditLogRepository.findAll().stream()
                .filter(a -> a.getPromptId().equals(UUID.fromString(promptId)))
                .filter(a -> a.getAction() == PromptAuditAction.VERSION_UPDATED)
                .toList();
        assertThat(audits).isNotEmpty();
        PromptAuditLog audit = audits.get(audits.size() - 1);
        assertThat(audit.getOldValue()).contains("contentHash=").contains("contentLength=");
        assertThat(audit.getNewValue()).contains("contentHash=").contains("changeSummary=sanitize audit");
        assertThat(audit.getOldValue()).doesNotContain("SECRET_TOKEN");
        assertThat(audit.getNewValue()).doesNotContain("UPDATED_SECRET");
        assertThat(audit.getOldValue()).doesNotContain(secretContent);
        assertThat(audit.getNewValue()).doesNotContain(updatedContent);
    }

    @Test
    void publishingSupersedesPreviousPublishedVersion() throws Exception {
        String uniqueName = "Supersede-" + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "promptType":"CHAT",
                                  "content":"v1 {{name}}",
                                  "variables":[{"name":"name","dataType":"STRING","required":true}]
                                }
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String promptId = created.get("id").asText();
        UUID firstVersionId = UUID.fromString(created.get("currentDraftVersionId").asText());

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + firstVersionId
                        + "/publish")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        MvcResult draftResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        String draftVersionId = objectMapper.readTree(draftResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + draftVersionId
                        + "/publish")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(promptVersionRepository.findById(firstVersionId).orElseThrow().getStatus())
                .isEqualTo(PromptVersionStatus.SUPERSEDED);
    }

    @Test
    void compareVersionsReturnsDiff() throws Exception {
        String uniqueName = "Compare-" + UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "promptType":"CHAT",
                                  "content":"Line one\\nLine two",
                                  "variables":[]
                                }
                                """.formatted(uniqueName)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String promptId = created.get("id").asText();
        String versionId = created.get("currentDraftVersionId").asText();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + versionId + "/publish")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        MvcResult draftResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        String draftVersionId = objectMapper.readTree(draftResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/versions/" + draftVersionId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Line one\\nLine changed",
                                  "variables":[]
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult compareResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/prompts/" + promptId + "/compare")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "leftVersionId":"%s",
                                  "rightVersionId":"%s"
                                }
                                """.formatted(versionId, draftVersionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diff").isArray())
                .andReturn();

        JsonNode compare = objectMapper.readTree(compareResult.getResponse().getContentAsString());
        assertThat(compare.get("leftContent").asText()).isNotEqualTo(compare.get("rightContent").asText());
        boolean hasChange = false;
        for (JsonNode line : compare.get("diff")) {
            if (!"UNCHANGED".equals(line.get("type").asText())) {
                hasChange = true;
                break;
            }
        }
        assertThat(hasChange).isTrue();
    }
}
