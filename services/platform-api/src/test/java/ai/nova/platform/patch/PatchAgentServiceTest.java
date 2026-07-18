package ai.nova.platform.patch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.dto.PatchDtos.PatchRunRequest;
import ai.nova.platform.patch.service.PatchAgentService;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFindingDraft;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTestDraft;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.entity.TestPriority;
import ai.nova.platform.testing.entity.TestType;
import ai.nova.platform.testing.service.TestingStorageService;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class PatchAgentServiceTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final String VALID_PATCH = PatchValidationTest.VALID_PATCH;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatchAgentService patchAgentService;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private ReviewStorageService reviewStorageService;

    @Autowired
    private TestingStorageService testingStorageService;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;
    private AuthenticatedUser user;

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
        user = new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("PATCH_RUN", "PATCH_READ"),
                true);
    }

    @Test
    void runPersistsPatchAndCallsModelOutsideTransaction() throws Exception {
        UUID taskId = createApprovedTask("patch-svc-" + UUID.randomUUID());
        AtomicBoolean sawOpenTx = new AtomicBoolean(false);
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenAnswer(invocation -> {
            sawOpenTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "summary", "Generated patch",
                    "filesChanged", 1,
                    "insertions", 1,
                    "deletions", 0,
                    "patch", VALID_PATCH,
                    "status", "VALID"));
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(body, 8, 12, 20, 4L));
        });

        PatchResult result = patchAgentService.run(new PatchRunRequest(taskId), user);
        assertThat(result.status().name()).isEqualTo("VALID");
        assertThat(result.statistics().filesChanged()).isEqualTo(1);
        assertThat(result.patch()).contains("LoginService.java");
        assertThat(sawOpenTx.get()).isFalse();

        ArgumentCaptor<ExecutionRequest> captor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(agentRuntimeClient).execute(captor.capture());
        assertThat(captor.getValue().systemPrompt()).contains("Patch Agent");
        assertThat(captor.getValue().messages().get(0).content()).contains("LoginService.java");
        assertThat(captor.getValue().messages().get(0).content()).contains("approved=true");
    }

    @Test
    void runRejectsWhenNoArtifacts() throws Exception {
        UUID taskId = createTask("patch-empty-" + UUID.randomUUID());
        assertThatThrownBy(() -> patchAgentService.run(new PatchRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PATCH_NO_ARTIFACTS");
    }

    @Test
    void runRejectsWhenReviewNotApproved() throws Exception {
        UUID taskId = createTaskWithArtifact("patch-unapproved-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(taskId, task.getOrganizationId());
        reviewStorageService.replaceReview(
                task,
                artifacts,
                new ParsedReviewOutput("Needs work", 40, false, List.of()),
                1L,
                "review-local",
                "LOCAL",
                2L);

        assertThatThrownBy(() -> patchAgentService.run(new PatchRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PATCH_NOT_APPROVED");
    }

    private UUID createApprovedTask(String name) throws Exception {
        UUID taskId = createTaskWithArtifact(name);
        var task = taskRepository.findById(taskId).orElseThrow();
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(taskId, task.getOrganizationId());
        reviewStorageService.replaceReview(
                task,
                artifacts,
                new ParsedReviewOutput(
                        "Approved",
                        92,
                        true,
                        List.of(new ReviewFindingDraft(
                                ReviewSeverity.LOW,
                                ReviewCategory.NAMING,
                                "Minor rename",
                                "Optional",
                                "Rename helper",
                                "src/LoginService.java"))),
                2L,
                "review-local",
                "LOCAL",
                3L);
        testingStorageService.replaceResult(
                task,
                artifacts,
                new ParsedTestingOutput(
                        "Unit tests",
                        84,
                        List.of(new GeneratedTestDraft(
                                TestType.UNIT,
                                TestPriority.HIGH,
                                "Login validation",
                                "Reject blank password",
                                "src/LoginService.java",
                                List.of()))),
                3L,
                "testing-local",
                "LOCAL",
                4L);
        return taskId;
    }

    private UUID createTaskWithArtifact(String name) throws Exception {
        UUID taskId = createTask(name);
        artifactStorageService.replaceArtifacts(
                taskRepository.findById(taskId).orElseThrow(),
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
        return taskId;
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
                                  "modelReference":"patch-local",
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
