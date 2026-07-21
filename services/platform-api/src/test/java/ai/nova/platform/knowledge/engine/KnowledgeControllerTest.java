package ai.nova.platform.knowledge.engine;

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

import com.jayway.jsonpath.JsonPath;

import ai.nova.platform.knowledge.engine.support.KnowledgeEngineTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = {"nova.knowledge.engine.enabled=true", "nova.audit.enabled=true"})
@AutoConfigureMockMvc
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String readToken;
    private String writeToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        readToken = jwtService.createAccessToken(KnowledgeEngineTestFixture.knowledgeReadUser());
        writeToken = jwtService.createAccessToken(KnowledgeEngineTestFixture.knowledgeWriteUser());
        adminToken = jwtService.createAccessToken(KnowledgeEngineTestFixture.knowledgeAdminUser());
    }

    @Test
    void configListCreateSearchAndArchive() throws Exception {
        mockMvc.perform(get("/api/knowledge/config").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.chunkSize").value(1000));

        String title = KnowledgeEngineTestFixture.uniqueTitle("ctrl-doc");
        MvcResult createResult = mockMvc.perform(post("/api/knowledge")
                        .header("Authorization", "Bearer " + writeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KnowledgeEngineTestFixture.createDocumentBody(title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn();

        String documentId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/knowledge/search")
                        .param("q", title)
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + documentId + "')]").exists());

        mockMvc.perform(get("/api/knowledge/" + documentId).header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId));

        mockMvc.perform(post("/api/knowledge/" + documentId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void createRequiresWritePermission() throws Exception {
        mockMvc.perform(post("/api/knowledge")
                        .header("Authorization", "Bearer " + readToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(KnowledgeEngineTestFixture.createDocumentBody(
                                KnowledgeEngineTestFixture.uniqueTitle("denied"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
