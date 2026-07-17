package ai.nova.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class KnowledgeDocumentControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
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
    void uploadReprocessListChunksAndRejectPdf() throws Exception {
        String body = "Nova knowledge document about retrieval augmented generation.\n\nSecond paragraph.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", body.getBytes(StandardCharsets.UTF_8));
        String docKey = "DOC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MvcResult uploadResult = mockMvc.perform(multipart(
                                "/api/projects/" + PROJECT_ID + "/knowledge-bases/" + DEMO_KB_ID + "/documents")
                        .file(file)
                        .param("documentKey", docKey)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.chunkCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();

        JsonNode uploaded = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String documentId = uploaded.get("id").asText();
        assertThat(uploaded.has("extractedText")).isFalse();

        mockMvc.perform(get("/api/projects/"
                                + PROJECT_ID
                                + "/knowledge-bases/"
                                + DEMO_KB_ID
                                + "/documents/"
                                + documentId
                                + "/chunks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").isString())
                .andExpect(jsonPath("$[0].contentHash").isString());

        mockMvc.perform(post("/api/projects/"
                                + PROJECT_ID
                                + "/knowledge-bases/"
                                + DEMO_KB_ID
                                + "/documents/"
                                + documentId
                                + "/reprocess")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", "%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(
                                "/api/projects/" + PROJECT_ID + "/knowledge-bases/" + DEMO_KB_ID + "/documents")
                        .file(pdf)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENT_TYPE_UNSUPPORTED"));

        mockMvc.perform(delete("/api/projects/"
                                + PROJECT_ID
                                + "/knowledge-bases/"
                                + DEMO_KB_ID
                                + "/documents/"
                                + documentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsDuplicateContentHash() throws Exception {
        byte[] bytes = ("Unique duplicate content " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file1 = new MockMultipartFile("file", "a.txt", "text/plain", bytes);
        MockMultipartFile file2 = new MockMultipartFile("file", "b.txt", "text/plain", bytes);

        mockMvc.perform(multipart(
                                "/api/projects/" + PROJECT_ID + "/knowledge-bases/" + DEMO_KB_ID + "/documents")
                        .file(file1)
                        .param("documentKey", "DUP_A_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart(
                                "/api/projects/" + PROJECT_ID + "/knowledge-bases/" + DEMO_KB_ID + "/documents")
                        .file(file2)
                        .param("documentKey", "DUP_B_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_DUPLICATE_CONTENT"));
    }
}
