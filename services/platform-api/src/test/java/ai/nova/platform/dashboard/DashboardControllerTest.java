package ai.nova.platform.dashboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.dashboard.support.DashboardTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = {"nova.dashboard.enabled=true", "nova.audit.enabled=true"})
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String readToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        readToken = jwtService.createAccessToken(DashboardTestFixture.dashboardReadOnlyUser());
        adminToken = jwtService.createAccessToken(DashboardTestFixture.dashboardAdminUser());
    }

    @Test
    void snapshotAndSections() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview").exists())
                .andExpect(jsonPath("$.pipeline.stages.length()").value(14));

        mockMvc.perform(get("/api/dashboard/overview").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCount").exists());

        mockMvc.perform(get("/api/dashboard/pipeline").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages").isArray());

        mockMvc.perform(get("/api/dashboard/config").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshRateSeconds").value(30));
    }

    @Test
    void refreshRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/dashboard/refresh").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/dashboard/refresh").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshedAt").exists());
    }

    @Test
    void exportCsv() throws Exception {
        mockMvc.perform(get("/api/dashboard/export")
                        .param("format", "csv")
                        .param("section", "overview")
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("dashboard-overview.csv")));
    }
}
