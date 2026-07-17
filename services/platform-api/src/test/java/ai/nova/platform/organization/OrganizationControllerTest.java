package ai.nova.platform.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest {

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
        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        accessToken = body.get("accessToken").asText();
    }

    @Test
    void listAndCreateOrganization() throws Exception {
        mockMvc.perform(get("/api/organizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", "Nova")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Nova Demo Organization"));

        String unique = "Acme Labs " + java.util.UUID.randomUUID();
        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","slug":"acme-%s"}
                                """.formatted(unique, java.util.UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(unique))
                .andExpect(jsonPath("$.createdBy").isNotEmpty())
                .andExpect(jsonPath("$.updatedBy").isNotEmpty());
    }

    @Test
    void rejectsDuplicateOrganizationName() throws Exception {
        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nova Demo Organization","slug":"nova-demo-2"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateOrganization() throws Exception {
        String id = "11111111-1111-1111-1111-111111111111";
        mockMvc.perform(put("/api/organizations/" + id)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nova Demo Organization","slug":"nova-demo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("nova-demo"));
    }

    @Test
    void deleteRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/organizations/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isUnauthorized());
    }
}
