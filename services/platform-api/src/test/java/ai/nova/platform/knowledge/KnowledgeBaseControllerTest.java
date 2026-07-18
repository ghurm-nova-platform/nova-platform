package ai.nova.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.repository.KnowledgeBaseRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeBaseControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_KB_ID = "88888888-8888-8888-8888-888888888801";
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
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
    void listsProvidersAndSeededKnowledgeBase() throws Exception {
        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/knowledge-bases/providers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[?(@.providerKey == 'DETERMINISTIC_LOCAL')]").exists());

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/knowledge-bases")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", "PRODUCT_DOCUMENTATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DEMO_KB_ID));
    }

    @Test
    void createUpdateActivateAndArchiveKnowledgeBase() throws Exception {
        String key = "KB_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "_");
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/knowledge-bases")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeKey":"%s",
                                  "name":"Docs",
                                  "description":"Test KB",
                                  "embeddingProviderKey":"DETERMINISTIC_LOCAL",
                                  "chunkSize":500,
                                  "chunkOverlap":50,
                                  "defaultTopK":3,
                                  "minimumScore":0.0
                                }
                                """.formatted(key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.embeddingModel").value("deterministic-v1"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String id = created.get("id").asText();
        int version = created.get("version").asInt();

        mockMvc.perform(put("/api/projects/" + PROJECT_ID + "/knowledge-bases/" + id)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":%d,
                                  "name":"Docs updated",
                                  "description":"Updated",
                                  "chunkSize":600,
                                  "chunkOverlap":60,
                                  "defaultTopK":4,
                                  "minimumScore":0.1
                                }
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Docs updated"));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/knowledge-bases/" + id + "/activate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/projects/" + PROJECT_ID + "/knowledge-bases/" + id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(knowledgeBaseRepository.findById(UUID.fromString(id)))
                .isPresent()
                .get()
                .extracting(kb -> kb.getStatus())
                .isEqualTo(KnowledgeBaseStatus.ARCHIVED);
    }

    @Test
    void hidesCrossTenantKnowledgeBases() throws Exception {
        String foreignToken = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "foreign@nova.local",
                "Foreign",
                java.util.List.of("ORG_ADMIN"),
                java.util.List.of(),
                true));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/knowledge-bases/" + DEMO_KB_ID)
                        .header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isNotFound());
    }
}
