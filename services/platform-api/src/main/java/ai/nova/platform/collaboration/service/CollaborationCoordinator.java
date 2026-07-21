package ai.nova.platform.collaboration.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.entity.CollaborationParticipantEntity;
import ai.nova.platform.collaboration.entity.CollaborationParticipantRole;
import ai.nova.platform.collaboration.entity.CollaborationParticipantStatus;
import ai.nova.platform.collaboration.entity.CollaborationSessionEntity;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskEntity;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventType;
import ai.nova.platform.collaboration.repository.CollaborationTaskRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class CollaborationCoordinator {

    private static final List<CollaborationTaskStatus> CONFLICT_STATUSES = List.of(
            CollaborationTaskStatus.ASSIGNED, CollaborationTaskStatus.IN_PROGRESS);

    private final CollaborationPersistenceSupport persistence;
    private final CollaborationReferenceValidator referenceValidator;
    private final CollaborationStateTransitionValidator stateValidator;
    private final CollaborationTaskRepository taskRepository;
    private final CollaborationTimelineService timelineService;
    private final ObjectMapper objectMapper;

    public CollaborationCoordinator(
            CollaborationPersistenceSupport persistence,
            CollaborationReferenceValidator referenceValidator,
            CollaborationStateTransitionValidator stateValidator,
            CollaborationTaskRepository taskRepository,
            CollaborationTimelineService timelineService,
            ObjectMapper objectMapper) {
        this.persistence = persistence;
        this.referenceValidator = referenceValidator;
        this.stateValidator = stateValidator;
        this.taskRepository = taskRepository;
        this.timelineService = timelineService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CollaborationTaskEntity handleTaskAction(
            CollaborationSessionEntity session, AssignTaskRequest request) {
        requireMutableSession(session);
        CollaborationTaskEntity task = referenceValidator.requireTask(session, request.taskId(), session.getOrganizationId());
        TaskAction action = request.action() == null ? TaskAction.ASSIGN : request.action();
        stateValidator.requireTaskAction(task.getStatus(), action);

        return switch (action) {
            case ASSIGN -> assignTask(session, task, request.participantId(), request.artifactRef(), request.parallelGroup());
            case COMPLETE -> completeTask(session, task, request.participantId());
            case REJECT -> rejectTask(session, task, request.reason());
            case REASSIGN -> reassignTask(session, task, request.reassignToParticipantId(), request.artifactRef(), request.parallelGroup());
            case BLOCK -> blockTask(session, task, request.blockedByTaskId(), request.reason());
            case RESUME -> resumeTask(session, task);
            case CANCEL -> cancelTask(session, task, request.reason());
        };
    }

    @Transactional
    public CollaborationTaskEntity assignTask(
            CollaborationSessionEntity session,
            CollaborationTaskEntity task,
            UUID participantId,
            String artifactRef,
            String parallelGroup) {
        requireMutableSession(session);
        stateValidator.requireTaskAction(task.getStatus(), TaskAction.ASSIGN);
        if (participantId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "COLLABORATION_INVALID_REQUEST", "participantId is required");
        }

        ensureDependenciesMet(session, task);
        ensureParallelGroupReady(session, task);

        CollaborationParticipantEntity participant =
                referenceValidator.requireParticipant(session, participantId, session.getOrganizationId());
        ensureParticipantAvailable(session, participantId, task.getId());

        Instant now = Instant.now();
        task.setParticipantId(participant.getId());
        task.setStatus(CollaborationTaskStatus.ASSIGNED);
        task.setAssignedAt(now);
        if (artifactRef != null && !artifactRef.isBlank()) {
            task.setArtifactRef(artifactRef);
        }
        if (parallelGroup != null && !parallelGroup.isBlank()) {
            task.setParallelGroup(parallelGroup);
        }
        task.setUpdatedAt(now);
        persistence.saveTask(task);

        participant.setCurrentTaskId(task.getId());
        participant.setStatus(CollaborationParticipantStatus.ACTIVE);
        if (participant.getStartedAt() == null) {
            participant.setStartedAt(now);
        }
        participant.setUpdatedAt(now);
        persistence.saveParticipant(participant);

        activateSessionIfNeeded(session, now);
        detectConflicts(session);

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.TASK_ASSIGNED,
                "Task assigned: " + task.getTitle(),
                participant.getParticipantRole(),
                task.getId(),
                null,
                null,
                Map.of("participantId", participant.getId().toString(), "taskKey", task.getTaskKey()));

        return task;
    }

    @Transactional
    public CollaborationTaskEntity completeTask(
            CollaborationSessionEntity session, CollaborationTaskEntity task, UUID participantId) {
        requireMutableSession(session);
        stateValidator.requireTaskAction(task.getStatus(), TaskAction.COMPLETE);

        CollaborationParticipantEntity participant = resolveParticipantForTask(session, task, participantId);
        Instant now = Instant.now();

        task.setStatus(CollaborationTaskStatus.COMPLETED);
        task.setCompletedAt(now);
        task.setCompletedByParticipantId(participant.getId());
        task.setUpdatedAt(now);
        persistence.saveTask(task);

        refreshParticipantAfterTaskChange(session, participant, task.getId(), now);
        detectConflicts(session);
        refreshSessionCompletion(session, now);

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.TASK_COMPLETED,
                "Task completed: " + task.getTitle(),
                participant.getParticipantRole(),
                task.getId(),
                null,
                null,
                Map.of("taskKey", task.getTaskKey()));

        return task;
    }

    @Transactional
    public CollaborationTaskEntity rejectTask(
            CollaborationSessionEntity session, CollaborationTaskEntity task, String reason) {
        requireMutableSession(session);
        stateValidator.requireTaskAction(task.getStatus(), TaskAction.REJECT);

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.REJECTED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        persistence.saveTask(task);

        if (task.getParticipantId() != null) {
            CollaborationParticipantEntity participant = referenceValidator.requireParticipant(
                    session, task.getParticipantId(), session.getOrganizationId());
            refreshParticipantAfterTaskChange(session, participant, task.getId(), now);
        }
        detectConflicts(session);

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.FAILED,
                "Task rejected: " + task.getTitle(),
                null,
                task.getId(),
                null,
                null,
                reason == null ? Map.of() : Map.of("reason", reason));

        return task;
    }

    @Transactional
    public CollaborationTaskEntity reassignTask(
            CollaborationSessionEntity session,
            CollaborationTaskEntity task,
            UUID reassignToParticipantId,
            String artifactRef,
            String parallelGroup) {
        requireMutableSession(session);
        stateValidator.requireTaskAction(task.getStatus(), TaskAction.REASSIGN);
        if (reassignToParticipantId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "COLLABORATION_INVALID_REQUEST", "reassignToParticipantId is required");
        }

        UUID previousParticipantId = task.getParticipantId();
        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.PENDING);
        task.setParticipantId(null);
        task.setAssignedAt(null);
        task.setStartedAt(null);
        task.setUpdatedAt(now);
        persistence.saveTask(task);

        if (previousParticipantId != null) {
            CollaborationParticipantEntity previousParticipant = referenceValidator.requireParticipant(
                    session, previousParticipantId, session.getOrganizationId());
            refreshParticipantAfterTaskChange(session, previousParticipant, task.getId(), now);
        }

        return assignTask(session, task, reassignToParticipantId, artifactRef, parallelGroup);
    }

    @Transactional
    public CollaborationTaskEntity blockTask(
            CollaborationSessionEntity session,
            CollaborationTaskEntity task,
            UUID blockedByTaskId,
            String reason) {
        requireMutableSession(session);
        stateValidator.requireTaskAction(task.getStatus(), TaskAction.BLOCK);
        if (blockedByTaskId != null) {
            referenceValidator.requireDependencyTask(session, blockedByTaskId, session.getOrganizationId());
        }

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.BLOCKED);
        task.setBlockedByTaskId(blockedByTaskId);
        task.setUpdatedAt(now);
        persistence.saveTask(task);

        if (task.getParticipantId() != null) {
            CollaborationParticipantEntity participant = referenceValidator.requireParticipant(
                    session, task.getParticipantId(), session.getOrganizationId());
            participant.setStatus(CollaborationParticipantStatus.BLOCKED);
            participant.setUpdatedAt(now);
            persistence.saveParticipant(participant);
        }

        stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.BLOCKED);
        session.setStatus(CollaborationSessionStatus.BLOCKED);
        session.setUpdatedAt(now);
        persistence.saveSession(session);

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.TASK_BLOCKED,
                "Task blocked: " + task.getTitle(),
                null,
                task.getId(),
                null,
                null,
                reason == null ? Map.of() : Map.of("reason", reason));

        return task;
    }

    @Transactional
    public CollaborationTaskEntity resumeTask(CollaborationSessionEntity session, CollaborationTaskEntity task) {
        requireMutableSession(session);
        stateValidator.requireTaskAction(task.getStatus(), TaskAction.RESUME);

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.IN_PROGRESS);
        task.setBlockedByTaskId(null);
        task.setUpdatedAt(now);
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        persistence.saveTask(task);

        if (task.getParticipantId() != null) {
            CollaborationParticipantEntity participant = referenceValidator.requireParticipant(
                    session, task.getParticipantId(), session.getOrganizationId());
            participant.setStatus(CollaborationParticipantStatus.ACTIVE);
            participant.setCurrentTaskId(task.getId());
            participant.setUpdatedAt(now);
            persistence.saveParticipant(participant);
        }

        if (session.getStatus() == CollaborationSessionStatus.BLOCKED) {
            stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.ACTIVE);
            session.setStatus(CollaborationSessionStatus.ACTIVE);
            session.setUpdatedAt(now);
            persistence.saveSession(session);
        }

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.TASK_RESUMED,
                "Task resumed: " + task.getTitle(),
                null,
                task.getId(),
                null,
                null,
                Map.of());

        return task;
    }

    @Transactional
    public CollaborationTaskEntity cancelTask(
            CollaborationSessionEntity session, CollaborationTaskEntity task, String reason) {
        stateValidator.requireTaskAction(task.getStatus(), TaskAction.CANCEL);

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.CANCELLED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        persistence.saveTask(task);

        if (task.getParticipantId() != null) {
            CollaborationParticipantEntity participant = referenceValidator.requireParticipant(
                    session, task.getParticipantId(), session.getOrganizationId());
            refreshParticipantAfterTaskChange(session, participant, task.getId(), now);
        }

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.CANCELLED,
                "Task cancelled: " + task.getTitle(),
                null,
                task.getId(),
                null,
                null,
                reason == null ? Map.of() : Map.of("reason", reason));

        return task;
    }

    @Transactional
    public void detectConflicts(CollaborationSessionEntity session) {
        List<CollaborationTaskEntity> activeTasks = taskRepository.findBySessionIdAndOrganizationIdAndStatusIn(
                session.getId(), session.getOrganizationId(), CONFLICT_STATUSES);

        Map<String, List<CollaborationTaskEntity>> byArtifact = new HashMap<>();
        for (CollaborationTaskEntity task : activeTasks) {
            if (task.getArtifactRef() == null || task.getArtifactRef().isBlank()) {
                continue;
            }
            byArtifact.computeIfAbsent(task.getArtifactRef(), ignored -> new ArrayList<>()).add(task);
        }

        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<CollaborationTaskEntity>> entry : byArtifact.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add(Map.of(
                        "artifactRef",
                        entry.getKey(),
                        "taskIds",
                        entry.getValue().stream().map(CollaborationTaskEntity::getId).map(UUID::toString).toList()));
            }
        }

        boolean conflictDetected = !conflicts.isEmpty();
        session.setConflictDetected(conflictDetected);
        if (conflictDetected) {
            stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.BLOCKED);
            session.setStatus(CollaborationSessionStatus.BLOCKED);
            session.setConflictDetailsJson(serializeConflictDetails(conflicts));
            timelineService.append(
                    session.getId(),
                    session.getOrganizationId(),
                    CollaborationTimelineEventType.CONFLICT,
                    "Artifact conflict detected",
                    null,
                    null,
                    null,
                    null,
                    Map.of("conflicts", conflicts));
        } else {
            session.setConflictDetailsJson(null);
            if (session.getStatus() == CollaborationSessionStatus.BLOCKED && !hasBlockedTasks(session)) {
                stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.ACTIVE);
                session.setStatus(CollaborationSessionStatus.ACTIVE);
            }
        }
        session.setUpdatedAt(Instant.now());
        persistence.saveSession(session);
    }

    @Transactional
    public void pauseSession(CollaborationSessionEntity session) {
        stateValidator.requireMutableSession(session.getStatus());
        stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.WAITING);

        Instant now = Instant.now();
        session.setStatus(CollaborationSessionStatus.WAITING);
        session.setUpdatedAt(now);
        persistence.saveSession(session);

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.PAUSED,
                "Collaboration paused",
                CollaborationParticipantRole.HUMAN_REVIEWER,
                null,
                null,
                null,
                Map.of());
    }

    @Transactional
    public void resumeSession(CollaborationSessionEntity session) {
        CollaborationSessionStatus target = session.isConflictDetected()
                ? CollaborationSessionStatus.BLOCKED
                : CollaborationSessionStatus.ACTIVE;
        stateValidator.requireSessionTransition(session.getStatus(), target);

        Instant now = Instant.now();
        session.setStatus(target);
        session.setUpdatedAt(now);
        persistence.saveSession(session);

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.RESUMED,
                "Collaboration resumed",
                CollaborationParticipantRole.HUMAN_REVIEWER,
                null,
                null,
                null,
                Map.of());
    }

    @Transactional
    public void cancelSession(CollaborationSessionEntity session) {
        stateValidator.requireMutableSession(session.getStatus());
        stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.CANCELLED);

        Instant now = Instant.now();
        List<CollaborationTaskEntity> tasks = taskRepository.findBySessionIdAndOrganizationIdAndStatusIn(
                session.getId(), session.getOrganizationId(), stateValidator.activeTaskStatuses());
        for (CollaborationTaskEntity task : tasks) {
            cancelTask(session, task, "Session cancelled");
        }

        session.setStatus(CollaborationSessionStatus.CANCELLED);
        session.setCompletedAt(now);
        session.setUpdatedAt(now);
        persistence.saveSession(session);

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.CANCELLED,
                "Collaboration session cancelled",
                null,
                null,
                null,
                null,
                Map.of());
    }

    private void ensureParticipantAvailable(CollaborationSessionEntity session, UUID participantId, UUID excludingTaskId) {
        List<CollaborationTaskEntity> activeTasks = taskRepository.findBySessionIdAndOrganizationIdAndParticipantIdAndStatusIn(
                session.getId(), session.getOrganizationId(), participantId, stateValidator.activeTaskStatuses());
        boolean busy = activeTasks.stream().anyMatch(task -> !task.getId().equals(excludingTaskId));
        if (busy) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_PARTICIPANT_BUSY",
                    "Participant already has an active task in this session");
        }
    }

    private void refreshParticipantAfterTaskChange(
            CollaborationSessionEntity session,
            CollaborationParticipantEntity participant,
            UUID affectedTaskId,
            Instant now) {
        if (affectedTaskId.equals(participant.getCurrentTaskId())) {
            participant.setCurrentTaskId(null);
        }

        List<CollaborationTaskEntity> remainingActive = taskRepository.findBySessionIdAndOrganizationIdAndParticipantIdAndStatusIn(
                session.getId(), session.getOrganizationId(), participant.getId(), stateValidator.activeTaskStatuses());

        if (remainingActive.isEmpty()) {
            List<CollaborationTaskEntity> participantTasks = taskRepository.findBySessionIdAndOrganizationIdAndParticipantId(
                    session.getId(), session.getOrganizationId(), participant.getId());
            boolean allTerminal = !participantTasks.isEmpty()
                    && participantTasks.stream().allMatch(task -> stateValidator.isTerminalTask(task.getStatus()));
            if (allTerminal) {
                participant.setStatus(CollaborationParticipantStatus.COMPLETED);
                participant.setProgressPercent(100);
                participant.setCompletedAt(now);
            } else {
                participant.setStatus(CollaborationParticipantStatus.IDLE);
                participant.setProgressPercent(0);
                participant.setCompletedAt(null);
            }
        } else {
            CollaborationTaskEntity current = remainingActive.getFirst();
            participant.setCurrentTaskId(current.getId());
            participant.setStatus(participantStatusForTask(current.getStatus()));
            participant.setCompletedAt(null);
        }
        participant.setUpdatedAt(now);
        persistence.saveParticipant(participant);
    }

    private CollaborationParticipantStatus participantStatusForTask(CollaborationTaskStatus taskStatus) {
        return switch (taskStatus) {
            case BLOCKED -> CollaborationParticipantStatus.BLOCKED;
            case ASSIGNED, IN_PROGRESS -> CollaborationParticipantStatus.ACTIVE;
            default -> CollaborationParticipantStatus.IDLE;
        };
    }

    private void ensureDependenciesMet(CollaborationSessionEntity session, CollaborationTaskEntity task) {
        if (task.getDependsOnTaskId() == null) {
            return;
        }
        CollaborationTaskEntity dependency = referenceValidator.requireDependencyTask(
                session, task.getDependsOnTaskId(), session.getOrganizationId());
        if (dependency.getStatus() != CollaborationTaskStatus.COMPLETED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_DEPENDENCY_NOT_MET",
                    "Dependency task must be completed before assignment");
        }
    }

    private void ensureParallelGroupReady(CollaborationSessionEntity session, CollaborationTaskEntity task) {
        if (task.getParallelGroup() == null || task.getParallelGroup().isBlank()) {
            return;
        }
        List<CollaborationTaskEntity> groupTasks = taskRepository.findBySessionIdAndOrganizationIdAndParallelGroup(
                session.getId(), session.getOrganizationId(), task.getParallelGroup());
        for (CollaborationTaskEntity groupTask : groupTasks) {
            if (groupTask.getId().equals(task.getId())) {
                continue;
            }
            if (groupTask.getDependsOnTaskId() != null) {
                CollaborationTaskEntity dependency = referenceValidator.requireDependencyTask(
                        session, groupTask.getDependsOnTaskId(), session.getOrganizationId());
                if (dependency.getStatus() != CollaborationTaskStatus.COMPLETED) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "COLLABORATION_PARALLEL_GROUP_BLOCKED",
                            "Parallel group has unmet dependencies");
                }
            }
        }
    }

    private void activateSessionIfNeeded(CollaborationSessionEntity session, Instant now) {
        if (session.getStatus() == CollaborationSessionStatus.CREATED
                || session.getStatus() == CollaborationSessionStatus.STARTING) {
            stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.ACTIVE);
            session.setStatus(CollaborationSessionStatus.ACTIVE);
            if (session.getStartedAt() == null) {
                session.setStartedAt(now);
            }
            session.setUpdatedAt(now);
            persistence.saveSession(session);
            timelineService.append(
                    session.getId(),
                    session.getOrganizationId(),
                    CollaborationTimelineEventType.STARTED,
                    "Collaboration session started",
                    null,
                    null,
                    null,
                    null,
                    Map.of());
        }
    }

    private void refreshSessionCompletion(CollaborationSessionEntity session, Instant now) {
        List<CollaborationTaskEntity> tasks = taskRepository.findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(
                session.getId(), session.getOrganizationId());
        if (tasks.isEmpty()) {
            return;
        }
        boolean allTerminal = tasks.stream().allMatch(task -> stateValidator.isTerminalTask(task.getStatus()));
        if (allTerminal && session.getStatus() != CollaborationSessionStatus.COMPLETED) {
            stateValidator.requireSessionTransition(session.getStatus(), CollaborationSessionStatus.COMPLETED);
            session.setStatus(CollaborationSessionStatus.COMPLETED);
            session.setCompletedAt(now);
            session.setUpdatedAt(now);
            persistence.saveSession(session);
            timelineService.append(
                    session.getId(),
                    session.getOrganizationId(),
                    CollaborationTimelineEventType.COMPLETED,
                    "Collaboration session completed",
                    null,
                    null,
                    null,
                    null,
                    Map.of());
        }
    }

    private boolean hasBlockedTasks(CollaborationSessionEntity session) {
        return !taskRepository
                .findBySessionIdAndOrganizationIdAndStatusIn(
                        session.getId(), session.getOrganizationId(), List.of(CollaborationTaskStatus.BLOCKED))
                .isEmpty();
    }

    private CollaborationParticipantEntity resolveParticipantForTask(
            CollaborationSessionEntity session, CollaborationTaskEntity task, UUID participantId) {
        if (participantId != null) {
            return referenceValidator.requireParticipant(session, participantId, session.getOrganizationId());
        }
        if (task.getParticipantId() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "COLLABORATION_INVALID_REQUEST", "Task has no assigned participant");
        }
        return referenceValidator.requireParticipant(session, task.getParticipantId(), session.getOrganizationId());
    }

    private void requireMutableSession(CollaborationSessionEntity session) {
        stateValidator.requireMutableSession(session.getStatus());
        if (session.getStatus() == CollaborationSessionStatus.WAITING) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "COLLABORATION_SESSION_PAUSED", "Session is paused");
        }
    }

    private String serializeConflictDetails(List<Map<String, Object>> conflicts) {
        try {
            return objectMapper.writeValueAsString(Map.of("conflicts", conflicts));
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
