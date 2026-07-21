package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.prreview.support.PrReviewTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = {"nova.pr-review.enabled=true", "nova.audit.enabled=true"})
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String readToken;
    private String runToken;

    @BeforeEach
    void setUp() {
        Mockito.doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        readToken = jwtService.createAccessToken(PrReviewTestFixture.prReviewReadUser());
        runToken = jwtService.createAccessToken(PrReviewTestFixture.prReviewRunUser());
    }

    @Test
    void configRunGetFindingsRecommendationsRiskKnowledgeAndExport() throws Exception {
        mockMvc.perform(get("/api/pr-review/config").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.maxFindings").exists());

        String escapedDiff = PrReviewTestFixture.securityDiff()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        MvcResult runResult = mockMvc.perform(post("/api/pr-review/run")
                        .header("Authorization", "Bearer " + runToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PrReviewTestFixture.runRequestBody(escapedDiff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value("REJECTED"))
                .andExpect(jsonPath("$.riskScore").exists())
                .andExpect(jsonPath("$.findings.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();

        String runId = JsonPath.read(runResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/pr-review/" + runId).header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(runId));

        mockMvc.perform(get("/api/pr-review/" + runId + "/findings").header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(get("/api/pr-review/" + runId + "/recommendations")
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pr-review/" + runId + "/risk-score")
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").exists())
                .andExpect(jsonPath("$.riskScore").exists());

        mockMvc.perform(get("/api/pr-review/" + runId + "/knowledge")
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pr-review/" + runId + "/export")
                        .header("Authorization", "Bearer " + runToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"format\":\"markdown\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pr-review")
                        .param("projectId", PrReviewTestFixture.PROJECT_ID.toString())
                        .header("Authorization", "Bearer " + readToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pr-review/" + runId + "/rerun")
                        .header("Authorization", "Bearer " + runToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void runRequiresPermission() throws Exception {
        mockMvc.perform(post("/api/pr-review/run")
                        .header("Authorization", "Bearer " + readToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PrReviewTestFixture.runRequestBody("ok")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PR_REVIEW_PERMISSION_DENIED"));
    }

    @Test
    void exportRequiresExportPermission() throws Exception {
        String escapedDiff = "public class Ok { int x = 1; }"
                .replace("\"", "\\\"");
        MvcResult runResult = mockMvc.perform(post("/api/pr-review/run")
                        .header("Authorization", "Bearer " + runToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PrReviewTestFixture.runRequestBody(escapedDiff)))
                .andExpect(status().isOk())
                .andReturn();
        String runId = JsonPath.read(runResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/pr-review/" + runId + "/export")
                        .header("Authorization", "Bearer " + readToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"format\":\"json\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PR_REVIEW_PERMISSION_DENIED"));
    }
}
