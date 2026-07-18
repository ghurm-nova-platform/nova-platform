package ai.nova.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.execution.BoundedOrchestrationExecutionDispatcher;
import ai.nova.platform.orchestration.execution.OrchestrationExecutionDispatcher;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskAttemptRepository;
import ai.nova.platform.orchestration.service.OrchestrationExecutionService;
import ai.nova.platform.orchestration.service.TaskClaimService;

@SpringBootTest
@AutoConfigureMockMvc
class OrchestrationConcurrencyTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskClaimService claimService;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private AgentTaskAttemptRepository attemptRepository;

    @Autowired
    private OrchestrationProperties properties;

    @Autowired
    private OrchestrationExecutionService executionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;
    private int previousGlobalConcurrency;

    @BeforeEach
    void setUp() throws Exception {
        previousGlobalConcurrency = properties.getGlobalConcurrency();
        properties.setGlobalConcurrency(20);
        neutralizeLeftoverOrchestrationWork();

        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        doAnswer(invocation -> RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("ok", 1, 1, 2, 5L)))
                .when(agentRuntimeClient)
                .execute(any(ExecutionRequest.class));

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

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        properties.setGlobalConcurrency(previousGlobalConcurrency);
        neutralizeLeftoverOrchestrationWork();
    }

    private void neutralizeLeftoverOrchestrationWork() {
        jdbcTemplate.update("""
                UPDATE agent_orchestration_tasks
                SET status = 'CANCELLED',
                    claimed_by = NULL,
                    claimed_at = NULL,
                    claim_expires_at = NULL,
                    cancelled_at = CURRENT_TIMESTAMP,
                    completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE status IN ('DRAFT', 'BLOCKED', 'READY', 'CLAIMED', 'RUNNING', 'RETRY_WAIT',
                                 'WAITING_APPROVAL', 'CANCEL_REQUESTED')
                """);
        jdbcTemplate.update("""
                UPDATE agent_orchestration_runs
                SET status = 'CANCELLED',
                    cancelled_at = CURRENT_TIMESTAMP,
                    completed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE status IN ('DRAFT', 'READY', 'RUNNING', 'WAITING', 'CANCEL_REQUESTED')
                """);
    }

    @Test
    void maxParallelOnePreventsOverlappingClaims() throws Exception {
        String runId = createStartedRun("cap-1", 1, 3);

        var first = claimService.claimReadyTasks(10);
        assertThat(first).hasSize(1);

        var second = claimService.claimReadyTasks(10);
        assertThat(second).isEmpty();

        assertThat(countByStatus(runId, TaskStatus.READY)).isEqualTo(2);
        assertThat(countByStatus(runId, TaskStatus.CLAIMED)).isEqualTo(1);
    }

    @Test
    void maxParallelTwoAllowsTwoClaimsAndKeepsThirdReady() throws Exception {
        String runId = createStartedRun("cap-2", 2, 3);

        var claimed = claimService.claimReadyTasks(10);
        assertThat(claimed).hasSize(2);
        assertThat(countByStatus(runId, TaskStatus.READY)).isEqualTo(1);
        assertThat(countByStatus(runId, TaskStatus.CLAIMED)).isEqualTo(2);
    }

    @Test
    void twoNodesCannotExceedPerRunCapacity() throws Exception {
        String runId = createStartedRun("cap-nodes", 1, 3);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<List<AgentOrchestrationTask>>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return claimService.claimReadyTasks(10);
                }));
            }
            start.countDown();
            List<AgentOrchestrationTask> all = new ArrayList<>();
            for (Future<List<AgentOrchestrationTask>> future : futures) {
                all.addAll(future.get(10, TimeUnit.SECONDS));
            }
            assertThat(all).hasSize(1);
            assertThat(countByStatus(runId, TaskStatus.CLAIMED)).isEqualTo(1);
            assertThat(countByStatus(runId, TaskStatus.READY)).isEqualTo(2);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void globalConcurrencyLimitIsEnforced() throws Exception {
        properties.setGlobalConcurrency(2);
        String runA = createStartedRun("global-a", 5, 3);
        String runB = createStartedRun("global-b", 5, 3);

        var claimed = claimService.claimReadyTasks(20);
        assertThat(claimed).hasSize(2);
        long active = taskRepository.countActiveSlotsGlobal();
        assertThat(active).isEqualTo(2);
        assertThat(countByStatus(runA, TaskStatus.READY) + countByStatus(runB, TaskStatus.READY))
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void cancelledRunTasksAreNeverClaimed() throws Exception {
        String runId = createStartedRun("cancel-claim", 2, 2);
        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/cancel")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"stop\"}"))
                .andExpect(status().isOk());

        var claimed = claimService.claimReadyTasks(10);
        assertThat(claimed).noneMatch(task -> task.getRunId().equals(UUID.fromString(runId)));
        assertThat(taskRepository.findByRunIdAndOrganizationId(
                        UUID.fromString(runId), UUID.fromString("11111111-1111-1111-1111-111111111111")))
                .noneMatch(t -> t.getStatus() == TaskStatus.READY || t.getStatus() == TaskStatus.CLAIMED);
    }

    @Test
    void executorRejectionReleasesClaimWithoutAttempt() throws Exception {
        createStartedRun("reject", 2, 2);
        var claimed = claimService.claimReadyTasks(2);
        assertThat(claimed).hasSize(2);

        CountDownLatch hold = new CountDownLatch(1);
        OrchestrationExecutionDispatcher dispatcher = new BoundedOrchestrationExecutionDispatcher(
                new java.util.concurrent.ThreadPoolExecutor(
                        1,
                        1,
                        60L,
                        TimeUnit.SECONDS,
                        new java.util.concurrent.SynchronousQueue<>(),
                        new java.util.concurrent.ThreadPoolExecutor.AbortPolicy()));

        boolean firstAccepted = dispatcher.dispatch(claimed.get(0).getId(), () -> {
            try {
                hold.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(firstAccepted).isTrue();
        // Ensure the single worker is busy before the second dispatch.
        Thread.sleep(50);

        boolean secondAccepted = dispatcher.dispatch(claimed.get(1).getId(), () -> {
        });
        assertThat(secondAccepted).isFalse();

        boolean released = claimService.releaseUnstartedClaim(claimed.get(1).getId());
        assertThat(released).isTrue();

        AgentOrchestrationTask releasedTask = taskRepository.findById(claimed.get(1).getId()).orElseThrow();
        assertThat(releasedTask.getStatus()).isEqualTo(TaskStatus.READY);
        assertThat(attemptRepository.findByTaskIdAndOrganizationIdOrderByAttemptNumberAsc(
                        releasedTask.getId(), releasedTask.getOrganizationId()))
                .isEmpty();

        hold.countDown();
    }

    @Test
    void maxParallelTwoAllowsOverlappingExecution() throws Exception {
        String runId = createStartedRun("overlap", 2, 2);
        AtomicInteger concurrent = new AtomicInteger();
        AtomicInteger maxConcurrent = new AtomicInteger();
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        doAnswer(invocation -> {
            int now = concurrent.incrementAndGet();
            maxConcurrent.accumulateAndGet(now, Math::max);
            bothStarted.countDown();
            release.await(10, TimeUnit.SECONDS);
            concurrent.decrementAndGet();
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("ok", 1, 1, 2, 5L));
        }).when(agentRuntimeClient).execute(any(ExecutionRequest.class));

        var claimed = claimService.claimReadyTasks(2);
        assertThat(claimed).hasSize(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (AgentOrchestrationTask task : claimed) {
                pool.submit(() -> executionService.executeClaimedTask(task.getId()));
            }
            assertThat(bothStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(maxConcurrent.get()).isEqualTo(2);
            release.countDown();
        } finally {
            pool.shutdown();
            pool.awaitTermination(15, TimeUnit.SECONDS);
        }
        assertThat(countByStatus(runId, TaskStatus.SUCCEEDED)).isEqualTo(2);
    }

    private long countByStatus(String runId, TaskStatus status) {
        return taskRepository.findByRunIdAndOrganizationId(
                        UUID.fromString(runId),
                        UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    private String createStartedRun(String name, int maxParallel, int taskCount) throws Exception {
        MvcResult create = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"%s",
                                  "objective":"concurrency test",
                                  "executionMode":"DEPENDENCY_GRAPH",
                                  "failurePolicy":"CONTINUE_INDEPENDENT",
                                  "maxParallelTasks":%d,
                                  "maximumDurationMs":3600000
                                }
                                """.formatted(PROJECT_ID, name + "-" + UUID.randomUUID(), maxParallel)))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText();

        for (int i = 1; i <= taskCount; i++) {
            mockMvc.perform(post("/api/orchestration-runs/" + runId + "/tasks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "taskKey":"t-%d",
                                      "displayName":"Task %d",
                                      "taskType":"AGENT_TURN",
                                      "assignedAgentId":"%s",
                                      "maxAttempts":1,
                                      "retryBackoffMs":1000,
                                      "priority":%d,
                                      "timeoutSeconds":60
                                    }
                                    """.formatted(i, i, DEMO_AGENT_ID, i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/ready")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        MvcResult started = mockMvc.perform(post("/api/orchestration-runs/" + runId + "/start")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(started.getResponse().getContentAsString());
        assertThat(body.get("status").asText()).isIn("RUNNING", "WAITING");
        return runId;
    }
}
