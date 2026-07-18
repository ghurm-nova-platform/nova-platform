package ai.nova.platform.orchestration.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.web.error.ApiException;

@Component
public class OrchestrationStateMachine {

    private static final Map<RunStatus, Set<RunStatus>> RUN_TRANSITIONS = new EnumMap<>(RunStatus.class);
    private static final Map<TaskStatus, Set<TaskStatus>> TASK_TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        RUN_TRANSITIONS.put(RunStatus.DRAFT, EnumSet.of(RunStatus.READY, RunStatus.CANCELLED));
        RUN_TRANSITIONS.put(RunStatus.READY, EnumSet.of(RunStatus.RUNNING, RunStatus.CANCEL_REQUESTED));
        RUN_TRANSITIONS.put(
                RunStatus.RUNNING,
                EnumSet.of(
                        RunStatus.WAITING,
                        RunStatus.SUCCEEDED,
                        RunStatus.PARTIALLY_SUCCEEDED,
                        RunStatus.FAILED,
                        RunStatus.TIMED_OUT,
                        RunStatus.CANCEL_REQUESTED));
        RUN_TRANSITIONS.put(
                RunStatus.WAITING,
                EnumSet.of(
                        RunStatus.RUNNING,
                        RunStatus.SUCCEEDED,
                        RunStatus.PARTIALLY_SUCCEEDED,
                        RunStatus.FAILED,
                        RunStatus.TIMED_OUT,
                        RunStatus.CANCEL_REQUESTED));
        RUN_TRANSITIONS.put(RunStatus.CANCEL_REQUESTED, EnumSet.of(RunStatus.CANCELLED));
        RUN_TRANSITIONS.put(RunStatus.SUCCEEDED, EnumSet.of(RunStatus.ARCHIVED));
        RUN_TRANSITIONS.put(RunStatus.PARTIALLY_SUCCEEDED, EnumSet.of(RunStatus.ARCHIVED));
        RUN_TRANSITIONS.put(RunStatus.FAILED, EnumSet.of(RunStatus.ARCHIVED));
        RUN_TRANSITIONS.put(RunStatus.CANCELLED, EnumSet.of(RunStatus.ARCHIVED));
        RUN_TRANSITIONS.put(RunStatus.TIMED_OUT, EnumSet.of(RunStatus.ARCHIVED));
        RUN_TRANSITIONS.put(RunStatus.ARCHIVED, EnumSet.noneOf(RunStatus.class));

        TASK_TRANSITIONS.put(TaskStatus.DRAFT, EnumSet.of(TaskStatus.BLOCKED, TaskStatus.READY, TaskStatus.CANCEL_REQUESTED, TaskStatus.CANCELLED));
        TASK_TRANSITIONS.put(TaskStatus.BLOCKED, EnumSet.of(TaskStatus.READY, TaskStatus.SKIPPED, TaskStatus.CANCEL_REQUESTED, TaskStatus.CANCELLED));
        TASK_TRANSITIONS.put(TaskStatus.READY, EnumSet.of(TaskStatus.CLAIMED, TaskStatus.CANCEL_REQUESTED, TaskStatus.CANCELLED));
        TASK_TRANSITIONS.put(TaskStatus.CLAIMED, EnumSet.of(TaskStatus.RUNNING, TaskStatus.READY, TaskStatus.CANCEL_REQUESTED, TaskStatus.CANCELLED));
        TASK_TRANSITIONS.put(
                TaskStatus.RUNNING,
                EnumSet.of(
                        TaskStatus.SUCCEEDED,
                        TaskStatus.FAILED,
                        TaskStatus.RETRY_WAIT,
                        TaskStatus.WAITING_APPROVAL,
                        TaskStatus.TIMED_OUT,
                        TaskStatus.CANCEL_REQUESTED));
        TASK_TRANSITIONS.put(TaskStatus.RETRY_WAIT, EnumSet.of(TaskStatus.READY, TaskStatus.CANCEL_REQUESTED, TaskStatus.CANCELLED));
        TASK_TRANSITIONS.put(TaskStatus.WAITING_APPROVAL, EnumSet.of(TaskStatus.READY, TaskStatus.SUCCEEDED, TaskStatus.FAILED, TaskStatus.CANCEL_REQUESTED, TaskStatus.CANCELLED));
        TASK_TRANSITIONS.put(TaskStatus.SUCCEEDED, EnumSet.noneOf(TaskStatus.class));
        TASK_TRANSITIONS.put(TaskStatus.FAILED, EnumSet.of(TaskStatus.READY)); // manual retry
        TASK_TRANSITIONS.put(TaskStatus.SKIPPED, EnumSet.noneOf(TaskStatus.class));
        TASK_TRANSITIONS.put(TaskStatus.CANCEL_REQUESTED, EnumSet.of(TaskStatus.CANCELLED));
        TASK_TRANSITIONS.put(TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class));
        TASK_TRANSITIONS.put(TaskStatus.TIMED_OUT, EnumSet.noneOf(TaskStatus.class));
    }

    public void transitionRun(RunStatus from, RunStatus to) {
        if (from == to) {
            return;
        }
        Set<RunStatus> allowed = RUN_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(RunStatus.class));
        if (!allowed.contains(to)) {
            throw invalid(from.name(), to.name());
        }
    }

    public void transitionTask(TaskStatus from, TaskStatus to) {
        if (from == to) {
            return;
        }
        Set<TaskStatus> allowed = TASK_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TaskStatus.class));
        if (!allowed.contains(to)) {
            throw invalid(from.name(), to.name());
        }
    }

    public boolean canTransitionRun(RunStatus from, RunStatus to) {
        if (from == to) {
            return true;
        }
        return RUN_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(RunStatus.class)).contains(to);
    }

    public boolean canTransitionTask(TaskStatus from, TaskStatus to) {
        if (from == to) {
            return true;
        }
        return TASK_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TaskStatus.class)).contains(to);
    }

    private static ApiException invalid(String from, String to) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "INVALID_STATE_TRANSITION",
                "Invalid state transition from " + from + " to " + to);
    }
}
