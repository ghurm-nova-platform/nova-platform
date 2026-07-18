package ai.nova.platform.knowledge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeSecurityTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void rejectsMissingPermissionForCreate() throws Exception {
        String token = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "user@nova.local",
                "User",
                List.of("USER"),
                List.of("KNOWLEDGE_READ"),
                true));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/knowledge-bases")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "knowledgeKey":"NOPE",
                                  "name":"Nope",
                                  "embeddingProviderKey":"DETERMINISTIC_LOCAL"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsReadWithKnowledgeRead() throws Exception {
        String token = jwtService.createAccessToken(new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "user@nova.local",
                "User",
                List.of("USER"),
                List.of("KNOWLEDGE_READ"),
                true));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/knowledge-bases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
