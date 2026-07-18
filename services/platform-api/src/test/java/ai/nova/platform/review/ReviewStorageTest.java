package ai.nova.platform.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFindingDraft;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.review.repository.ReviewFindingRepository;
import ai.nova.platform.review.repository.ReviewResultRepository;
import ai.nova.platform.review.service.ReviewStorageService;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewStorageTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private ReviewStorageService reviewStorageService;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private ReviewResultRepository resultRepository;

    @Autowired
    private ReviewFindingRepository findingRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());

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
    void persistsScoreApprovalFindingsAndArtifactReferences() throws Exception {
        UUID taskId = createTask("review-store-" + UUID.randomUUID());
        AgentOrchestrationTask task = taskRepository.findById(taskId).orElseThrow();
        var artifacts = artifactStorageService.replaceArtifacts(
                task,
                List.of(new GeneratedArtifactDraft(
                        ArtifactType.SOURCE_FILE,
                        ArtifactLanguage.JAVA,
                        "src/LoginService.java",
                        "LoginService.java",
                        "class LoginService {}")),
                10L,
                "coding-local",
                "LOCAL",
                5L);

        ReviewResult stored = reviewStorageService.replaceReview(
                task,
                artifacts,
                new ParsedReviewOutput(
                        "Overall code quality is good.",
                        92,
                        true,
                        List.of(new ReviewFindingDraft(
                                ReviewSeverity.MEDIUM,
                                ReviewCategory.SECURITY,
                                "Input validation",
                                "Request payload is not validated.",
                                "Add Bean Validation.",
                                "src/LoginService.java"))),
                30L,
                "review-local",
                "LOCAL",
                12L);

        assertThat(stored.score()).isEqualTo(92);
        assertThat(stored.approved()).isTrue();
        assertThat(stored.findings()).hasSize(1);
        assertThat(stored.reviewedArtifacts()).hasSize(1);
        assertThat(stored.severityCounts().get("MEDIUM")).isEqualTo(1L);
        assertThat(resultRepository.findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
                        taskId, task.getOrganizationId()))
                .isPresent();
        assertThat(findingRepository.findByReviewResultIdOrderBySeverityDescTitleAsc(stored.id())).hasSize(1);

        reviewStorageService.replaceReview(
                task,
                artifacts,
                new ParsedReviewOutput("Second review", 88, false, List.of()),
                20L,
                "review-local",
                "LOCAL",
                8L);
        assertThat(resultRepository.findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
                        taskId, task.getOrganizationId()))
                .get()
                .extracting(r -> r.getScore())
                .isEqualTo(88);
    }

    private UUID createTask(String name) throws Exception {
        MvcResult runResult = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"%s",
                                  "objective":"Build login",
                                  "executionMode":"SEQUENTIAL",
                                  "failurePolicy":"FAIL_FAST",
                                  "maxParallelTasks":1,
                                  "maximumDurationMs":60000
                                }
                                """.formatted(PROJECT_ID, name)))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = objectMapper.readTree(runResult.getResponse().getContentAsString()).get("id").asText();
        MvcResult taskResult = mockMvc.perform(post("/api/orchestration-runs/" + runId + "/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskKey":"coding-1",
                                  "displayName":"Generate login",
                                  "description":"agentRole=coding",
                                  "taskType":"AGENT_TURN",
                                  "modelReference":"coding-local",
                                  "maxAttempts":1,
                                  "retryBackoffMs":0,
                                  "priority":1,
                                  "timeoutSeconds":60,
                                  "idempotencyKey":"coding-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode task = objectMapper.readTree(taskResult.getResponse().getContentAsString());
        return UUID.fromString(task.get("id").asText());
    }
}
