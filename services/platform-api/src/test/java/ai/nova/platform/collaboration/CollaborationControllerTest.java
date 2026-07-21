package ai.nova.platform.collaboration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import java.util.UUID;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
@AutoConfigureMockMvc
class CollaborationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String readToken;
    private String writeToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        readToken = jwtService.createAccessToken(CollaborationTestFixture.collaborationReadUser());
        writeToken = jwtService.createAccessToken(CollaborationTestFixture.collaborationWriteUser());
        adminToken = jwtService.createAccessToken(CollaborationTestFixture.collaborationAdminUser());
    }

    @Test
    void listConfigCreateAndGetDetail() throws Exception {
        mockMvc.perform(get("/api/collaboration/config").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.pollingSeconds").exists());

        String name = CollaborationTestFixture.uniqueName("ctrl-session");
        MvcResult createResult = mockMvc.perform(post("/api/collaboration")
                        .header("Authorization", "Bearer " + writeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CollaborationTestFixture.createSessionBody(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.tasks.length()").value(2))
                .andReturn();

        String sessionId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/collaboration")
                        .param("projectId", CollaborationTestFixture.PROJECT_ID.toString())
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + sessionId + "')]").exists());

        mockMvc.perform(get("/api/collaboration/" + sessionId).header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    void assignMessageTimelineAndPause() throws Exception {
        String name = CollaborationTestFixture.uniqueName("ctrl-flow");
        MvcResult createResult = mockMvc.perform(post("/api/collaboration")
                        .header("Authorization", "Bearer " + writeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CollaborationTestFixture.createSessionBody(name)))
                .andExpect(status().isOk())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        String sessionId = JsonPath.read(body, "$.id");
        String taskId = JsonPath.read(body, "$.tasks[0].id");
        String participantId = JsonPath.read(body, "$.participants[0].id");

        mockMvc.perform(post("/api/collaboration/" + sessionId + "/assign")
                        .header("Authorization", "Bearer " + writeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CollaborationTestFixture.assignTaskBody(
                                UUID.fromString(taskId), UUID.fromString(participantId), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].status").value("ASSIGNED"));

        mockMvc.perform(post("/api/collaboration/" + sessionId + "/message")
                        .header("Authorization", "Bearer " + writeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CollaborationTestFixture.sendMessageBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1));

        mockMvc.perform(get("/api/collaboration/" + sessionId + "/timeline")
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType=='CREATED')]").exists())
                .andExpect(jsonPath("$[?(@.eventType=='TASK_ASSIGNED')]").exists())
                .andExpect(jsonPath("$[?(@.eventType=='MESSAGE_SENT')]").exists());

        mockMvc.perform(post("/api/collaboration/" + sessionId + "/pause")
                        .header("Authorization", "Bearer " + writeToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/collaboration/" + sessionId + "/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void createRequiresWritePermission() throws Exception {
        mockMvc.perform(post("/api/collaboration")
                        .header("Authorization", "Bearer " + readToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CollaborationTestFixture.createSessionBody(CollaborationTestFixture.uniqueName("denied"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
