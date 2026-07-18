package ai.nova.platform.orchestration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.execution.service.ExecutionLifecycleService;
import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskAttempt;
import ai.nova.platform.orchestration.entity.AttemptStatus;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskAttemptRepository;
import ai.nova.platform.orchestration.service.OrchestrationEventService;
import ai.nova.platform.orchestration.service.OrchestrationExecutionService;
import ai.nova.platform.orchestration.service.OrchestrationRunFinalizer;
import ai.nova.platform.orchestration.service.OrchestrationSchedulingService;
import ai.nova.platform.orchestration.service.OrchestrationStateMachine;
import ai.nova.platform.orchestration.service.TaskInputResolver;
import ai.nova.platform.orchestration.service.TaskRetryPolicy;

@ExtendWith(MockitoExtension.class)
class OrchestrationExecutionStaleTest {

    @Mock
    private AgentOrchestrationRunRepository runRepository;
    @Mock
    private AgentOrchestrationTaskRepository taskRepository;
    @Mock
    private AgentTaskAttemptRepository attemptRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private OrchestrationEventService eventService;
    @Mock
    private OrchestrationSchedulingService schedulingService;
    @Mock
    private OrchestrationRunFinalizer runFinalizer;
    @Mock
    private ExecutionLifecycleService executionLifecycleService;
    @Mock
    private AgentRuntimeClient agentRuntimeClient;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private TransactionStatus transactionStatus;

    private OrchestrationExecutionService executionService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        OrchestrationProperties properties = new OrchestrationProperties();
        executionService = new OrchestrationExecutionService(
                runRepository,
                taskRepository,
                attemptRepository,
                agentRepository,
                new OrchestrationStateMachine(),
                eventService,
                schedulingService,
                runFinalizer,
                new TaskRetryPolicy(properties),
                new TaskInputResolver(new ObjectMapper(), properties),
                executionLifecycleService,
                agentRuntimeClient,
                properties,
                new ObjectMapper(),
                transactionTemplate,
                clock);
    }

    @Test
    void lateSuccessAfterCancelIsIgnored() {
        UUID orgId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        AgentOrchestrationRun run = new AgentOrchestrationRun(
                runId,
                orgId,
                projectId,
                "run",
                "obj",
                RunStatus.CANCEL_REQUESTED,
                ExecutionMode.SEQUENTIAL,
                FailurePolicy.FAIL_FAST,
                1,
                3600000,
                UUID.randomUUID(),
                Instant.now(clock));
        AgentOrchestrationTask task = new AgentOrchestrationTask(
                taskId,
                orgId,
                projectId,
                runId,
                "t1",
                "T1",
                TaskType.AGENT_TURN,
                TaskStatus.CANCEL_REQUESTED,
                "t1",
                1,
                1000,
                100,
                60,
                run.getCreatedBy(),
                Instant.now(clock));
        task.setAttemptCount(1);

        AgentTaskAttempt attempt = new AgentTaskAttempt(
                attemptId,
                orgId,
                projectId,
                runId,
                taskId,
                1,
                AttemptStatus.STARTED,
                Instant.now(clock),
                "w",
                Instant.now(clock));

        OrchestrationExecutionService.Tx1Context ctx = new OrchestrationExecutionService.Tx1Context(
                taskId, runId, orgId, attemptId, 1, 1L, 1L, null, false, UUID.randomUUID());

        when(taskRepository.findByIdAndRunIdAndOrganizationId(taskId, runId, orgId)).thenReturn(Optional.of(task));
        when(runRepository.findByIdAndOrganizationId(runId, orgId)).thenReturn(Optional.of(run));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(agentRuntimeClient.execute(any()))
                .thenReturn(RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("late", 1, 1, 2, 10)));

        AtomicReference<OrchestrationExecutionService.Tx1Context> held = new AtomicReference<>(ctx);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            if (callback == null) {
                return held.get();
            }
            // TX1 path returns held context without calling real beginExecution
            return held.get();
        });
        org.mockito.Mockito.doAnswer(inv -> {
                    Consumer<TransactionStatus> action = inv.getArgument(0);
                    action.accept(transactionStatus);
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());

        executionService.executeClaimedTask(taskId);

        verify(schedulingService, never()).afterTaskTerminal(any(), any());
        verify(eventService)
                .appendEvent(
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.eq(OrchestrationEventType.TASK_STALE_RESULT_IGNORED),
                        any(),
                        any());
    }
}
