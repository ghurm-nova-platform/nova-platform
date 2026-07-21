package ai.nova.platform.collaboration.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.web.error.ApiException;

@Component
public class CollaborationStateTransitionValidator {

    private static final Set<CollaborationSessionStatus> TERMINAL_SESSION_STATUSES = EnumSet.of(
            CollaborationSessionStatus.COMPLETED,
            CollaborationSessionStatus.FAILED,
            CollaborationSessionStatus.CANCELLED);

    private static final Set<CollaborationTaskStatus> TERMINAL_TASK_STATUSES = EnumSet.of(
            CollaborationTaskStatus.COMPLETED,
            CollaborationTaskStatus.REJECTED,
            CollaborationTaskStatus.CANCELLED);

    private static final Set<CollaborationTaskStatus> ACTIVE_TASK_STATUSES = EnumSet.of(
            CollaborationTaskStatus.ASSIGNED,
            CollaborationTaskStatus.IN_PROGRESS,
            CollaborationTaskStatus.BLOCKED);

    private static final Map<CollaborationSessionStatus, Set<CollaborationSessionStatus>> SESSION_TRANSITIONS =
            Map.ofEntries(
                    Map.entry(
                            CollaborationSessionStatus.CREATED,
                            EnumSet.of(
                                    CollaborationSessionStatus.STARTING,
                                    CollaborationSessionStatus.ACTIVE,
                                    CollaborationSessionStatus.WAITING,
                                    CollaborationSessionStatus.BLOCKED,
                                    CollaborationSessionStatus.CANCELLED)),
                    Map.entry(
                            CollaborationSessionStatus.STARTING,
                            EnumSet.of(
                                    CollaborationSessionStatus.ACTIVE,
                                    CollaborationSessionStatus.WAITING,
                                    CollaborationSessionStatus.BLOCKED,
                                    CollaborationSessionStatus.CANCELLED)),
                    Map.entry(
                            CollaborationSessionStatus.ACTIVE,
                            EnumSet.of(
                                    CollaborationSessionStatus.WAITING,
                                    CollaborationSessionStatus.BLOCKED,
                                    CollaborationSessionStatus.COMPLETED,
                                    CollaborationSessionStatus.FAILED,
                                    CollaborationSessionStatus.CANCELLED)),
                    Map.entry(
                            CollaborationSessionStatus.WAITING,
                            EnumSet.of(
                                    CollaborationSessionStatus.ACTIVE,
                                    CollaborationSessionStatus.BLOCKED,
                                    CollaborationSessionStatus.CANCELLED)),
                    Map.entry(
                            CollaborationSessionStatus.BLOCKED,
                            EnumSet.of(
                                    CollaborationSessionStatus.ACTIVE,
                                    CollaborationSessionStatus.WAITING,
                                    CollaborationSessionStatus.COMPLETED,
                                    CollaborationSessionStatus.CANCELLED)));

    public void requireMutableSession(CollaborationSessionStatus status) {
        if (TERMINAL_SESSION_STATUSES.contains(status)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_SESSION_INVALID_STATUS",
                    "Session is terminal and cannot be modified");
        }
    }

    public void requireSessionTransition(CollaborationSessionStatus from, CollaborationSessionStatus to) {
        if (from == to) {
            return;
        }
        Set<CollaborationSessionStatus> allowed = SESSION_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_SESSION_INVALID_STATUS",
                    "Session cannot transition from " + from + " to " + to);
        }
    }

    public void requireMutableTask(CollaborationTaskStatus status) {
        if (TERMINAL_TASK_STATUSES.contains(status)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_TASK_INVALID_STATUS",
                    "Task is terminal and cannot be modified");
        }
    }

    public void requireTaskAction(CollaborationTaskStatus status, TaskAction action) {
        switch (action) {
            case ASSIGN -> {
                if (status != CollaborationTaskStatus.PENDING && status != CollaborationTaskStatus.ASSIGNED) {
                    invalidTaskAction(status, action);
                }
            }
            case COMPLETE -> {
                if (status != CollaborationTaskStatus.ASSIGNED && status != CollaborationTaskStatus.IN_PROGRESS) {
                    invalidTaskAction(status, action);
                }
            }
            case REJECT, CANCEL -> requireMutableTask(status);
            case REASSIGN -> {
                if (TERMINAL_TASK_STATUSES.contains(status)) {
                    invalidTaskAction(status, action);
                }
            }
            case BLOCK -> {
                if (TERMINAL_TASK_STATUSES.contains(status)) {
                    invalidTaskAction(status, action);
                }
            }
            case RESUME -> {
                if (status != CollaborationTaskStatus.BLOCKED) {
                    invalidTaskAction(status, action);
                }
            }
        }
    }

    public boolean isTerminalTask(CollaborationTaskStatus status) {
        return TERMINAL_TASK_STATUSES.contains(status);
    }

    public boolean isActiveTask(CollaborationTaskStatus status) {
        return ACTIVE_TASK_STATUSES.contains(status);
    }

    public Set<CollaborationTaskStatus> activeTaskStatuses() {
        return ACTIVE_TASK_STATUSES;
    }

    private void invalidTaskAction(CollaborationTaskStatus status, TaskAction action) {
        throw new ApiException(
                HttpStatus.CONFLICT,
                "COLLABORATION_TASK_INVALID_STATUS",
                "Task cannot be " + action.name().toLowerCase() + " in status " + status);
    }
}
