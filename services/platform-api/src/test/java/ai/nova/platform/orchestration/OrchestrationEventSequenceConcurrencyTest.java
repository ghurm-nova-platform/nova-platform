package ai.nova.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.orchestration.entity.AgentOrchestrationEvent;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.repository.AgentOrchestrationEventRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.service.OrchestrationEventService;
import ai.nova.platform.orchestration.service.TaskClaimService;

@SpringBootTest
@AutoConfigureMockMvc
class OrchestrationEventSequenceConcurrencyTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrchestrationEventService eventService;

    @Autowired
    private AgentOrchestrationEventRepository eventRepository;

    @Autowired
    private AgentOrchestrationRunRepository runRepository;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private TaskClaimService claimService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String accessToken;

    @BeforeEach
    void login() throws Exception {
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
    void concurrentAppendsProduceUniqueOrderedSequences() throws Exception {
        String runId = createDraftRun("seq-run");
        AgentOrchestrationRun run = runRepository.findById(UUID.fromString(runId)).orElseThrow();

        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                int idx = i;
                futures.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return transactionTemplate.execute(status -> {
                        AgentOrchestrationEvent event = eventService.appendEvent(
                                run.getId(),
                                run.getOrganizationId(),
                                run.getProjectId(),
                                null,
                                OrchestrationEventType.TASK_READY,
                                "{\"i\":" + idx + "}",
                                null);
                        return event.getEventSequence();
                    });
                }));
            }
            start.countDown();
            Set<Long> sequences = new HashSet<>();
            for (Future<Long> future : futures) {
                sequences.add(future.get(15, TimeUnit.SECONDS));
            }
            assertThat(sequences).hasSize(threads);

            List<AgentOrchestrationEvent> events =
                    eventRepository.findByRunIdAndOrganizationIdOrderByEventSequenceAsc(
                            UUID.fromString(runId), ORG_ID);
            List<Long> ordered = events.stream()
                    .filter(e -> e.getEventType() == OrchestrationEventType.TASK_READY)
                    .map(AgentOrchestrationEvent::getEventSequence)
                    .collect(Collectors.toList());
            assertThat(ordered).hasSize(threads);
            assertThat(ordered).isSorted();
            assertThat(new HashSet<>(ordered)).hasSize(threads);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void claimAndCompletionAreNotRolledBackByEventSequenceContention() throws Exception {
        String runId = createStartedRun("claim-seq", 2, 2);
        UUID runUuid = UUID.fromString(runId);
        AgentOrchestrationRun run = runRepository.findById(runUuid).orElseThrow();

        int noiseThreads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(noiseThreads + 2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger eventFailures = new AtomicInteger();
        List<Future<?>> noise = new ArrayList<>();
        try {
            for (int i = 0; i < noiseThreads; i++) {
                noise.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    for (int n = 0; n < 5; n++) {
                        try {
                            transactionTemplate.executeWithoutResult(status -> eventService.appendEvent(
                                    run.getId(),
                                    run.getOrganizationId(),
                                    run.getProjectId(),
                                    null,
                                    OrchestrationEventType.TASK_READY,
                                    null,
                                    null));
                        } catch (RuntimeException ex) {
                            eventFailures.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }

            Future<List<AgentOrchestrationTask>> claimFuture = pool.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return claimService.claimReadyTasks(2);
            });

            start.countDown();
            for (Future<?> future : noise) {
                future.get(20, TimeUnit.SECONDS);
            }
            List<AgentOrchestrationTask> claimed = claimFuture.get(20, TimeUnit.SECONDS);

            assertThat(eventFailures.get()).isZero();
            assertThat(claimed).hasSize(2);
            assertThat(claimed).allMatch(t -> t.getStatus() == TaskStatus.CLAIMED);

            List<AgentOrchestrationEvent> claimEvents =
                    eventRepository.findByRunIdAndOrganizationIdOrderByEventSequenceAsc(runUuid, ORG_ID).stream()
                            .filter(e -> e.getEventType() == OrchestrationEventType.TASK_CLAIMED)
                            .toList();
            assertThat(claimEvents).hasSize(2);
            assertThat(claimEvents.stream().map(AgentOrchestrationEvent::getEventSequence).collect(Collectors.toSet()))
                    .hasSize(2);
        } finally {
            pool.shutdownNow();
        }
    }

    private String createDraftRun(String name) throws Exception {
        MvcResult create = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"%s",
                                  "objective":"event sequence",
                                  "executionMode":"DEPENDENCY_GRAPH",
                                  "failurePolicy":"BEST_EFFORT",
                                  "maxParallelTasks":2,
                                  "maximumDurationMs":3600000
                                }
                                """.formatted(PROJECT_ID, name + "-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText();
    }

    private String createStartedRun(String name, int maxParallel, int taskCount) throws Exception {
        String runId = createDraftRun(name);
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
        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/start")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        return runId;
    }
}
