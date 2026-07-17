package ai.nova.platform.project;

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

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

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
        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        accessToken = body.get("accessToken").asText();
    }

    @Test
    void createUpdateAndArchiveProject() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Portal Redesign",
                                  "description":"UI refresh",
                                  "status":"ACTIVE",
                                  "visibility":"PRIVATE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Portal Redesign"))
                .andExpect(jsonPath("$.organizationId").value("11111111-1111-1111-1111-111111111111"))
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/projects/" + id)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Portal Redesign",
                                  "description":"Updated",
                                  "status":"DRAFT",
                                  "visibility":"INTERNAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.visibility").value("INTERNAL"));

        mockMvc.perform(delete("/api/projects/" + id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        Project archived = projectRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void listSupportsSearchAndRejectsDuplicateNames() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", "Demo")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Demo Project"));

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Demo Project",
                                  "description":"dup",
                                  "status":"ACTIVE",
                                  "visibility":"PRIVATE"
                                }
                                """))
                .andExpect(status().isConflict());
    }
}
