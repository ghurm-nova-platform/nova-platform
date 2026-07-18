package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskDependencyRepository;

@Service
public class OrchestrationSchedulingService {

    private static final Set<TaskStatus> TERMINAL = EnumSet.of(
            TaskStatus.SUCCEEDED,
            TaskStatus.FAILED,
            TaskStatus.SKIPPED,
            TaskStatus.CANCELLED,
            TaskStatus.TIMED_OUT);

    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentTaskDependencyRepository dependencyRepository;
    private final OrchestrationStateMachine stateMachine;
    private final OrchestrationEventService eventService;
    private final Clock clock;

    public OrchestrationSchedulingService(
            AgentOrchestrationTaskRepository taskRepository,
            AgentTaskDependencyRepository dependencyRepository,
            OrchestrationStateMachine stateMachine,
            OrchestrationEventService eventService,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.stateMachine = stateMachine;
        this.eventService = eventService;
        this.clock = clock;
    }

    @Transactional
    public void afterTaskTerminal(AgentOrchestrationRun run, AgentOrchestrationTask completed) {
        Instant now = Instant.now(clock);
        List<AgentTaskDependency> outgoing = dependencyRepository.findByPredecessorTaskId(completed.getId());
        Map<UUID, AgentOrchestrationTask> tasks = indexTasks(run);

        for (AgentTaskDependency dep : outgoing) {
            AgentOrchestrationTask successor = tasks.get(dep.getSuccessorTaskId());
            if (successor == null) {
                continue;
            }
            if (TERMINAL.contains(successor.getStatus())
                    || successor.getStatus() == TaskStatus.CANCEL_REQUESTED
                    || successor.getStatus() == TaskStatus.RUNNING
                    || successor.getStatus() == TaskStatus.CLAIMED) {
                continue;
            }

            boolean unlock;
            if (dep.getDependencyType() == DependencyType.SUCCESS) {
                unlock = completed.getStatus() == TaskStatus.SUCCEEDED;
                if (!unlock) {
                    if (successor.getStatus() == TaskStatus.BLOCKED || successor.getStatus() == TaskStatus.DRAFT || successor.getStatus() == TaskStatus.READY) {
                        skipTask(run, successor, now);
                    }
                    continue;
                }
            } else {
                unlock = TERMINAL.contains(completed.getStatus());
            }

            if (unlock && areDependenciesMet(successor, tasks) && successor.getStatus() == TaskStatus.BLOCKED) {
                if (run.getFailurePolicy() == FailurePolicy.FAIL_FAST
                        && hasFailedRequiredPredecessor(successor, tasks)) {
                    continue;
                }
                stateMachine.transitionTask(successor.getStatus(), TaskStatus.READY);
                successor.setStatus(TaskStatus.READY);
                successor.setUpdatedAt(now);
                taskRepository.save(successor);
                eventService.appendEvent(run, successor.getId(), OrchestrationEventType.TASK_READY, null, null);
            }
        }
    }

    @Transactional
    public void initializeRootTasks(AgentOrchestrationRun run) {
        Instant now = Instant.now(clock);
        Map<UUID, AgentOrchestrationTask> tasks = indexTasks(run);
        List<AgentTaskDependency> deps = dependencyRepository.findByRunId(run.getId());
        Set<UUID> successors = deps.stream()
                .map(AgentTaskDependency::getSuccessorTaskId)
                .collect(java.util.stream.Collectors.toSet());

        for (AgentOrchestrationTask task : tasks.values()) {
            if (successors.contains(task.getId())) {
                stateMachine.transitionTask(task.getStatus(), TaskStatus.BLOCKED);
                task.setStatus(TaskStatus.BLOCKED);
                task.setUpdatedAt(now);
                taskRepository.save(task);
                eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_BLOCKED, null, null);
            } else {
                stateMachine.transitionTask(task.getStatus(), TaskStatus.READY);
                task.setStatus(TaskStatus.READY);
                task.setUpdatedAt(now);
                taskRepository.save(task);
                eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_READY, null, null);
            }
        }
    }

    private boolean areDependenciesMet(AgentOrchestrationTask successor, Map<UUID, AgentOrchestrationTask> tasks) {
        List<AgentTaskDependency> incoming = dependencyRepository.findBySuccessorTaskId(successor.getId());
        for (AgentTaskDependency dep : incoming) {
            AgentOrchestrationTask pred = tasks.get(dep.getPredecessorTaskId());
            if (pred == null) {
                return false;
            }
            if (dep.getDependencyType() == DependencyType.SUCCESS) {
                if (pred.getStatus() != TaskStatus.SUCCEEDED) {
                    return false;
                }
            } else if (!TERMINAL.contains(pred.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasFailedRequiredPredecessor(
            AgentOrchestrationTask successor, Map<UUID, AgentOrchestrationTask> tasks) {
        for (AgentTaskDependency dep : dependencyRepository.findBySuccessorTaskId(successor.getId())) {
            AgentOrchestrationTask pred = tasks.get(dep.getPredecessorTaskId());
            if (pred != null
                    && dep.getDependencyType() == DependencyType.SUCCESS
                    && (pred.getStatus() == TaskStatus.FAILED || pred.getStatus() == TaskStatus.TIMED_OUT)) {
                return true;
            }
        }
        return false;
    }

    private void skipTask(AgentOrchestrationRun run, AgentOrchestrationTask task, Instant now) {
        stateMachine.transitionTask(task.getStatus(), TaskStatus.SKIPPED);
        task.setStatus(TaskStatus.SKIPPED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        taskRepository.save(task);
        eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_SKIPPED, null, null);
        afterTaskTerminal(run, task);
    }

    private Map<UUID, AgentOrchestrationTask> indexTasks(AgentOrchestrationRun run) {
        Map<UUID, AgentOrchestrationTask> map = new HashMap<>();
        for (AgentOrchestrationTask task :
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId())) {
            map.put(task.getId(), task);
        }
        return map;
    }
}
