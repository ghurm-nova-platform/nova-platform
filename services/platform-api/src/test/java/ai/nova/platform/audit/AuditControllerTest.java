package ai.nova.platform.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.service.AuditService;
import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = "nova.audit.enabled=true")
@AutoConfigureMockMvc
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuditService auditService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        accessToken = jwtService.createAccessToken(AuditTestFixture.auditAdminUser());
    }

    @Test
    void listGetHistoryAndSearch() throws Exception {
        UUID entityId = UUID.randomUUID();
        var recorded = auditService.record(AuditTestFixture.sampleEvent(entityId, AuditAction.CREATE));

        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray());

        mockMvc.perform(get("/api/audit/" + recorded.id()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recorded.id().toString()));

        mockMvc.perform(get("/api/audit/history")
                        .param("entityType", AuditEntityType.ENVIRONMENT.name())
                        .param("entityId", entityId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray());

        mockMvc.perform(get("/api/audit/search")
                        .param("entityType", AuditEntityType.ENVIRONMENT.name())
                        .param("entityId", entityId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].entityId").value(entityId.toString()));
    }

    @Test
    void notFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/audit/" + UUID.randomUUID()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUDIT_NOT_FOUND"));
    }
}
