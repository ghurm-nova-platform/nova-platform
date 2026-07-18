package ai.nova.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.modelcatalog.service.ModelReferenceResolver;
import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.orchestration.service.OrchestrationGraphValidator;
import ai.nova.platform.web.error.ApiException;

class OrchestrationGraphValidatorTest {

    private OrchestrationGraphValidator validator;
    private ModelReferenceResolver modelReferenceResolver;

    @BeforeEach
    void setUp() {
        OrchestrationProperties properties = new OrchestrationProperties();
        modelReferenceResolver = mock(ModelReferenceResolver.class);
        validator = new OrchestrationGraphValidator(properties, modelReferenceResolver);
    }

    @Test
    void rejectsEmptyGraph() {
        AgentOrchestrationRun run = run(ExecutionMode.DEPENDENCY_GRAPH);
        assertThatThrownBy(() -> validator.validate(run, List.of(), List.of()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("ORCHESTRATION_EMPTY");
    }

    @Test
    void rejectsSelfDependency() {
        AgentOrchestrationRun run = run(ExecutionMode.DEPENDENCY_GRAPH);
        AgentOrchestrationTask task = task(run, "t1", null);
        AgentTaskDependency dep = new AgentTaskDependency(
                run.getId(), task.getId(), task.getId(), DependencyType.SUCCESS, Instant.now());
        assertThatThrownBy(() -> validator.validate(run, List.of(task), List.of(dep)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("ORCHESTRATION_SELF_DEPENDENCY");
    }

    @Test
    void rejectsDirectCycle() {
        AgentOrchestrationRun run = run(ExecutionMode.DEPENDENCY_GRAPH);
        AgentOrchestrationTask a = task(run, "a", null);
        AgentOrchestrationTask b = task(run, "b", null);
        List<AgentTaskDependency> deps = List.of(
                new AgentTaskDependency(run.getId(), a.getId(), b.getId(), DependencyType.SUCCESS, Instant.now()),
                new AgentTaskDependency(run.getId(), b.getId(), a.getId(), DependencyType.SUCCESS, Instant.now()));
        assertThatThrownBy(() -> validator.validate(run, List.of(a, b), deps))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("ORCHESTRATION_GRAPH_CYCLE");
    }

    @Test
    void rejectsIndirectCycle() {
        AgentOrchestrationRun run = run(ExecutionMode.DEPENDENCY_GRAPH);
        AgentOrchestrationTask a = task(run, "a", null);
        AgentOrchestrationTask b = task(run, "b", null);
        AgentOrchestrationTask c = task(run, "c", null);
        List<AgentTaskDependency> deps = List.of(
                new AgentTaskDependency(run.getId(), a.getId(), b.getId(), DependencyType.SUCCESS, Instant.now()),
                new AgentTaskDependency(run.getId(), b.getId(), c.getId(), DependencyType.SUCCESS, Instant.now()),
                new AgentTaskDependency(run.getId(), c.getId(), a.getId(), DependencyType.SUCCESS, Instant.now()));
        assertThatThrownBy(() -> validator.validate(run, List.of(a, b, c), deps))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("ORCHESTRATION_GRAPH_CYCLE");
    }

    @Test
    void acceptsValidSequentialGraph() {
        AgentOrchestrationRun run = run(ExecutionMode.SEQUENTIAL);
        AgentOrchestrationTask task = task(run, "only", 1);
        when(modelReferenceResolver.resolve(any(), eq("ignored"))).thenThrow(new RuntimeException("should not call"));
        validator.validate(run, List.of(task), List.of());
    }

    private AgentOrchestrationRun run(ExecutionMode mode) {
        return new AgentOrchestrationRun(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "run",
                "objective",
                RunStatus.DRAFT,
                mode,
                FailurePolicy.FAIL_FAST,
                1,
                3600000,
                UUID.randomUUID(),
                Instant.now());
    }

    private AgentOrchestrationTask task(AgentOrchestrationRun run, String key, Integer sequence) {
        AgentOrchestrationTask task = new AgentOrchestrationTask(
                UUID.randomUUID(),
                run.getOrganizationId(),
                run.getProjectId(),
                run.getId(),
                key,
                key,
                TaskType.AGENT_TURN,
                TaskStatus.DRAFT,
                key,
                1,
                1000,
                100,
                60,
                run.getCreatedBy(),
                Instant.now());
        task.setAssignedAgentId(UUID.randomUUID());
        task.setSequenceOrder(sequence);
        return task;
    }
}
