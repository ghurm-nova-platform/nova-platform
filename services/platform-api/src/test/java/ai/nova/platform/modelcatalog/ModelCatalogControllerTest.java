package ai.nova.platform.modelcatalog;

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

@SpringBootTest
@AutoConfigureMockMvc
class ModelCatalogControllerTest {

    private static final String DEMO_PROVIDER_ID = "99999999-9999-9999-9999-999999999901";
    private static final String DEMO_MODEL_ID = "99999999-9999-9999-9999-999999999911";
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
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
    void listsSeededCatalogModel() throws Exception {
        mockMvc.perform(get("/api/ai-models")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", "deterministic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DEMO_MODEL_ID))
                .andExpect(jsonPath("$.content[0].modelKey").value("deterministic-chat-v1"))
                .andExpect(jsonPath("$.content[0].capabilities").isArray());
    }

    @Test
    void createsUpdatesActivatesAndManagesCapabilitiesAndAliases() throws Exception {
        String modelKey = "catalog-chat-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult create = mockMvc.perform(post("/api/ai-models")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId":"%s",
                                  "modelKey":"%s",
                                  "providerModelId":"%s-provider",
                                  "displayName":"Catalog Chat",
                                  "modelType":"CHAT",
                                  "contextWindowTokens":8192,
                                  "maxOutputTokens":2048,
                                  "capabilities":["CHAT","STREAMING"]
                                }
                                """.formatted(DEMO_PROVIDER_ID, modelKey, modelKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.modelKey").value(modelKey))
                .andExpect(jsonPath("$.capabilities[?(@.capability == 'CHAT')]").exists())
                .andReturn();

        JsonNode created = objectMapper.readTree(create.getResponse().getContentAsString());
        String modelId = created.get("id").asText();
        int version = created.get("version").asInt();

        mockMvc.perform(put("/api/ai-models/" + modelId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"Catalog Chat Updated",
                                  "contextWindowTokens":8192,
                                  "maxOutputTokens":2048,
                                  "version":%d
                                }
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Catalog Chat Updated"));

        mockMvc.perform(put("/api/ai-models/" + modelId + "/capabilities")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "capabilities":[
                                    {"capability":"CHAT","enabled":true},
                                    {"capability":"TOOL_CALLING","enabled":true},
                                    {"capability":"FUNCTION_CALLING","enabled":true}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportsTools").value(true));

        mockMvc.perform(post("/api/ai-models/" + modelId + "/activate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        MvcResult aliasResult = mockMvc.perform(post("/api/ai-models/" + modelId + "/aliases")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias":"Fast-Catalog"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.normalizedAlias").value("fast-catalog"))
                .andReturn();
        String aliasId = objectMapper.readTree(aliasResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/ai-models/" + modelId + "/aliases")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alias").value("Fast-Catalog"));

        mockMvc.perform(delete("/api/ai-model-aliases/" + aliasId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/ai-models/" + modelId + "/deprecate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPRECATED"));

        mockMvc.perform(post("/api/ai-models/" + modelId + "/archive")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void orgIsolationReturnsNotFoundForUnknownModel() throws Exception {
        mockMvc.perform(get("/api/ai-models/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MODEL_NOT_FOUND"));
    }

    @Test
    void rbacDeniesMemberWithoutCatalogCreate() throws Exception {
        String memberToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "member@nova.local",
                "Member",
                List.of("ORG_MEMBER"),
                List.of("MODEL_CATALOG_READ"),
                true));

        mockMvc.perform(get("/api/ai-models")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ai-models")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId":"%s",
                                  "modelKey":"denied-model-key",
                                  "providerModelId":"denied",
                                  "displayName":"Denied",
                                  "modelType":"CHAT",
                                  "contextWindowTokens":1024,
                                  "maxOutputTokens":256
                                }
                                """.formatted(DEMO_PROVIDER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
