package ai.nova.platform.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateDependencyRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateTaskRequest;
import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.orchestration.repository.AgentOrchestrationEventRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskDependencyRepository;
import ai.nova.platform.orchestration.service.OrchestrationGraphService;
import ai.nova.platform.orchestration.service.OrchestrationTaskService;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionDependency;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionTaskDefinition;
import ai.nova.platform.planner.dto.PlannerDtos.ImportPlanRequest;
import ai.nova.platform.planner.service.PlannerImportService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class PlannerImportAtomicityTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlannerImportService importService;

    @Autowired
    private AgentOrchestrationRunRepository runRepository;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private AgentTaskDependencyRepository dependencyRepository;

    @Autowired
    private AgentOrchestrationEventRepository eventRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @MockitoSpyBean
    private OrchestrationTaskService taskService;

    @MockitoSpyBean
    private OrchestrationGraphService graphService;

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
                List.of("PLANNER_PLAN", "PLANNER_IMPORT", "ORCHESTRATION_RUN_CREATE", "ORCHESTRATION_TASK_MANAGE"),
                true);

        org.mockito.Mockito.reset(taskService, graphService);
    }

    @Test
    void failureWhileCreatingTaskRollsBackRunAndEarlierTasks() {
        String runName = "atomic-fail-task-" + UUID.randomUUID();
        doThrow(new ApiException(HttpStatus.BAD_REQUEST, "FORCED_TASK_FAILURE", "boom"))
                .when(taskService)
                .create(any(), argThat((CreateTaskRequest req) -> "t2".equals(req.taskKey())), any());

        long runsBefore = runRepository.count();
        long tasksBefore = taskRepository.count();
        long eventsBefore = eventRepository.count();

        assertThatThrownBy(() -> importService.importPlan(
                        new ImportPlanRequest(UUID.fromString(PROJECT_ID), runName, twoTaskPlan()), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("FORCED_TASK_FAILURE");

        assertThat(runRepository.count()).isEqualTo(runsBefore);
        assertThat(taskRepository.count()).isEqualTo(tasksBefore);
        assertThat(eventRepository.count()).isEqualTo(eventsBefore);
        assertThat(runRepository.findAll().stream().noneMatch(r -> runName.equals(r.getName()))).isTrue();
    }

    @Test
    void failureWhileCreatingDependencyRollsBackRunTasksDepsAndEvents() {
        String runName = "atomic-fail-dep-" + UUID.randomUUID();
        doThrow(new ApiException(HttpStatus.BAD_REQUEST, "FORCED_DEP_FAILURE", "boom"))
                .when(graphService)
                .addDependency(any(), any(CreateDependencyRequest.class), any());

        long runsBefore = runRepository.count();
        long tasksBefore = taskRepository.count();
        long depsBefore = dependencyRepository.count();
        long eventsBefore = eventRepository.count();

        assertThatThrownBy(() -> importService.importPlan(
                        new ImportPlanRequest(UUID.fromString(PROJECT_ID), runName, twoTaskPlan()), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("FORCED_DEP_FAILURE");

        assertThat(runRepository.count()).isEqualTo(runsBefore);
        assertThat(taskRepository.count()).isEqualTo(tasksBefore);
        assertThat(dependencyRepository.count()).isEqualTo(depsBefore);
        assertThat(eventRepository.count()).isEqualTo(eventsBefore);
    }

    @Test
    void successfulImportPersistsCompleteDraftGraph() throws Exception {
        String runName = "atomic-ok-import-" + UUID.randomUUID();
        mockMvc.perform(post("/api/planner/import")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ImportPlanRequest(UUID.fromString(PROJECT_ID), runName, twoTaskPlan()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.taskStatusCounts.DRAFT").value(2));

        var run = runRepository.findAll().stream().filter(r -> runName.equals(r.getName())).findFirst().orElseThrow();
        assertThat(taskRepository.findByRunIdAndOrganizationId(run.getId(), ORG_ID)).hasSize(2);
        assertThat(dependencyRepository.findByRunId(run.getId())).hasSize(1);
        assertThat(eventRepository.findByRunIdAndOrganizationIdOrderByEventSequenceAsc(run.getId(), ORG_ID))
                .isNotEmpty();
    }

    @Test
    void successfulPlanAndCreatePersistsCompleteDraftGraphAndCallsModelOutsideTransaction()
            throws Exception {
        AtomicBoolean aiSawActiveTransaction = new AtomicBoolean(true);
        AtomicInteger aiCalls = new AtomicInteger();
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenAnswer(invocation -> {
            aiCalls.incrementAndGet();
            aiSawActiveTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(
                    objectMapper.writeValueAsString(twoTaskPlan()), 1, 1, 2, 5L));
        });

        String runName = "atomic-ok-plan-create-" + UUID.randomUUID();
        mockMvc.perform(post("/api/planner/plan-and-create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "objective":"Build authentication",
                                  "runName":"%s"
                                }
                                """.formatted(PROJECT_ID, runName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.draftRun.status").value("DRAFT"))
                .andExpect(jsonPath("$.draftRun.taskStatusCounts.DRAFT").value(2));

        assertThat(aiCalls.get()).isEqualTo(1);
        assertThat(aiSawActiveTransaction.get()).isFalse();

        var run = runRepository.findAll().stream().filter(r -> runName.equals(r.getName())).findFirst().orElseThrow();
        assertThat(taskRepository.findByRunIdAndOrganizationId(run.getId(), ORG_ID)).hasSize(2);
        assertThat(dependencyRepository.findByRunId(run.getId())).hasSize(1);
    }

    private static ExecutionPlan twoTaskPlan() {
        return new ExecutionPlan(
                "Build authentication",
                ExecutionMode.DEPENDENCY_GRAPH,
                FailurePolicy.FAIL_FAST,
                2,
                600000L,
                null,
                1000L,
                60L,
                0.01,
                null,
                List.of(
                        new ExecutionTaskDefinition(
                                "t1", "Task 1", null, TaskType.AGENT_TURN, "research", null, 1, null, null, null, null),
                        new ExecutionTaskDefinition(
                                "t2", "Task 2", null, TaskType.AGENT_TURN, "coding", null, 2, null, null, null, null)),
                List.of(new ExecutionDependency("t1", "t2", DependencyType.SUCCESS)),
                null);
    }
}
