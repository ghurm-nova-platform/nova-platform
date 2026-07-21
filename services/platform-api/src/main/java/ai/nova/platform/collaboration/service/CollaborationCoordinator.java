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
import ai.nova.platform.collaboration.repository.CollaborationParticipantRepository;
import ai.nova.platform.collaboration.repository.CollaborationSessionRepository;
import ai.nova.platform.collaboration.repository.CollaborationTaskRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class CollaborationCoordinator {

    private static final List<CollaborationTaskStatus> ACTIVE_TASK_STATUSES = List.of(
            CollaborationTaskStatus.ASSIGNED,
            CollaborationTaskStatus.IN_PROGRESS,
            CollaborationTaskStatus.BLOCKED);

    private static final List<CollaborationTaskStatus> CONFLICT_STATUSES = List.of(
            CollaborationTaskStatus.ASSIGNED, CollaborationTaskStatus.IN_PROGRESS);

    private final CollaborationSessionRepository sessionRepository;
    private final CollaborationParticipantRepository participantRepository;
    private final CollaborationTaskRepository taskRepository;
    private final CollaborationTimelineService timelineService;
    private final ObjectMapper objectMapper;

    public CollaborationCoordinator(
            CollaborationSessionRepository sessionRepository,
            CollaborationParticipantRepository participantRepository,
            CollaborationTaskRepository taskRepository,
            CollaborationTimelineService timelineService,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.taskRepository = taskRepository;
        this.timelineService = timelineService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CollaborationTaskEntity handleTaskAction(
            CollaborationSessionEntity session, AssignTaskRequest request) {
        requireActiveSession(session);
        CollaborationTaskEntity task = requireTask(session, request.taskId());
        TaskAction action = request.action() == null ? TaskAction.ASSIGN : request.action();

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
        requireActiveSession(session);
        if (participantId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "COLLABORATION_INVALID_REQUEST", "participantId is required");
        }
        if (task.getStatus() != CollaborationTaskStatus.PENDING && task.getStatus() != CollaborationTaskStatus.ASSIGNED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_TASK_INVALID_STATUS",
                    "Task cannot be assigned in status " + task.getStatus());
        }
        ensureDependenciesMet(session, task);
        ensureParallelGroupReady(session, task);

        CollaborationParticipantEntity participant = requireParticipant(session, participantId);
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
        taskRepository.save(task);

        participant.setCurrentTaskId(task.getId());
        participant.setStatus(CollaborationParticipantStatus.ACTIVE);
        if (participant.getStartedAt() == null) {
            participant.setStartedAt(now);
        }
        participant.setUpdatedAt(now);
        participantRepository.save(participant);

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
        requireActiveSession(session);
        if (task.getStatus() != CollaborationTaskStatus.ASSIGNED
                && task.getStatus() != CollaborationTaskStatus.IN_PROGRESS) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_TASK_INVALID_STATUS",
                    "Task cannot be completed in status " + task.getStatus());
        }

        CollaborationParticipantEntity participant = resolveParticipantForTask(session, task, participantId);
        Instant now = Instant.now();

        task.setStatus(CollaborationTaskStatus.COMPLETED);
        task.setCompletedAt(now);
        task.setCompletedByParticipantId(participant.getId());
        task.setUpdatedAt(now);
        taskRepository.save(task);

        participant.setCurrentTaskId(null);
        participant.setProgressPercent(100);
        participant.setStatus(CollaborationParticipantStatus.COMPLETED);
        participant.setCompletedAt(now);
        participant.setUpdatedAt(now);
        participantRepository.save(participant);

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
        requireActiveSession(session);
        if (task.getStatus() == CollaborationTaskStatus.COMPLETED
                || task.getStatus() == CollaborationTaskStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_TASK_INVALID_STATUS",
                    "Task cannot be rejected in status " + task.getStatus());
        }

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.REJECTED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        taskRepository.save(task);

        clearParticipantCurrentTask(session, task.getParticipantId(), now);
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
        if (reassignToParticipantId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "COLLABORATION_INVALID_REQUEST", "reassignToParticipantId is required");
        }
        clearParticipantCurrentTask(session, task.getParticipantId(), Instant.now());
        task.setStatus(CollaborationTaskStatus.PENDING);
        task.setParticipantId(null);
        task.setAssignedAt(null);
        task.setStartedAt(null);
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
        return assignTask(session, task, reassignToParticipantId, artifactRef, parallelGroup);
    }

    @Transactional
    public CollaborationTaskEntity blockTask(
            CollaborationSessionEntity session,
            CollaborationTaskEntity task,
            UUID blockedByTaskId,
            String reason) {
        requireActiveSession(session);
        if (task.getStatus() == CollaborationTaskStatus.COMPLETED
                || task.getStatus() == CollaborationTaskStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_TASK_INVALID_STATUS",
                    "Task cannot be blocked in status " + task.getStatus());
        }

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.BLOCKED);
        task.setBlockedByTaskId(blockedByTaskId);
        task.setUpdatedAt(now);
        taskRepository.save(task);

        if (task.getParticipantId() != null) {
            participantRepository
                    .findByIdAndOrganizationId(task.getParticipantId(), session.getOrganizationId())
                    .ifPresent(participant -> {
                        participant.setStatus(CollaborationParticipantStatus.BLOCKED);
                        participant.setUpdatedAt(now);
                        participantRepository.save(participant);
                    });
        }

        session.setStatus(CollaborationSessionStatus.BLOCKED);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

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
        if (task.getStatus() != CollaborationTaskStatus.BLOCKED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_TASK_INVALID_STATUS",
                    "Task is not blocked");
        }

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.IN_PROGRESS);
        task.setBlockedByTaskId(null);
        task.setUpdatedAt(now);
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        taskRepository.save(task);

        if (task.getParticipantId() != null) {
            participantRepository
                    .findByIdAndOrganizationId(task.getParticipantId(), session.getOrganizationId())
                    .ifPresent(participant -> {
                        participant.setStatus(CollaborationParticipantStatus.ACTIVE);
                        participant.setUpdatedAt(now);
                        participantRepository.save(participant);
                    });
        }

        if (session.getStatus() == CollaborationSessionStatus.BLOCKED) {
            session.setStatus(CollaborationSessionStatus.ACTIVE);
            session.setUpdatedAt(now);
            sessionRepository.save(session);
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
        if (task.getStatus() == CollaborationTaskStatus.COMPLETED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_TASK_INVALID_STATUS",
                    "Completed task cannot be cancelled");
        }

        Instant now = Instant.now();
        task.setStatus(CollaborationTaskStatus.CANCELLED);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        taskRepository.save(task);

        clearParticipantCurrentTask(session, task.getParticipantId(), now);

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
                session.setStatus(CollaborationSessionStatus.ACTIVE);
            }
        }
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void pauseSession(CollaborationSessionEntity session) {
        if (session.getStatus() == CollaborationSessionStatus.COMPLETED
                || session.getStatus() == CollaborationSessionStatus.CANCELLED
                || session.getStatus() == CollaborationSessionStatus.FAILED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_SESSION_INVALID_STATUS",
                    "Session cannot be paused in status " + session.getStatus());
        }

        Instant now = Instant.now();
        session.setStatus(CollaborationSessionStatus.WAITING);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

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
        if (session.getStatus() != CollaborationSessionStatus.WAITING
                && session.getStatus() != CollaborationSessionStatus.BLOCKED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_SESSION_INVALID_STATUS",
                    "Session cannot be resumed from status " + session.getStatus());
        }

        Instant now = Instant.now();
        session.setStatus(session.isConflictDetected()
                ? CollaborationSessionStatus.BLOCKED
                : CollaborationSessionStatus.ACTIVE);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

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
        if (session.getStatus() == CollaborationSessionStatus.COMPLETED
                || session.getStatus() == CollaborationSessionStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_SESSION_INVALID_STATUS",
                    "Session is already terminal");
        }

        Instant now = Instant.now();
        List<CollaborationTaskEntity> tasks = taskRepository.findBySessionIdAndOrganizationIdAndStatusIn(
                session.getId(), session.getOrganizationId(), ACTIVE_TASK_STATUSES);
        for (CollaborationTaskEntity task : tasks) {
            cancelTask(session, task, "Session cancelled");
        }

        session.setStatus(CollaborationSessionStatus.CANCELLED);
        session.setCompletedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

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

    private void ensureDependenciesMet(CollaborationSessionEntity session, CollaborationTaskEntity task) {
        if (task.getDependsOnTaskId() == null) {
            return;
        }
        CollaborationTaskEntity dependency = taskRepository
                .findByIdAndOrganizationId(task.getDependsOnTaskId(), session.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT, "COLLABORATION_DEPENDENCY_MISSING", "Dependency task not found"));
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
                CollaborationTaskEntity dependency = taskRepository
                        .findByIdAndOrganizationId(groupTask.getDependsOnTaskId(), session.getOrganizationId())
                        .orElse(null);
                if (dependency != null && dependency.getStatus() != CollaborationTaskStatus.COMPLETED) {
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
            session.setStatus(CollaborationSessionStatus.ACTIVE);
            if (session.getStartedAt() == null) {
                session.setStartedAt(now);
            }
            session.setUpdatedAt(now);
            sessionRepository.save(session);
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
        boolean allTerminal = tasks.stream().allMatch(this::isTerminalTask);
        if (allTerminal && session.getStatus() != CollaborationSessionStatus.COMPLETED) {
            session.setStatus(CollaborationSessionStatus.COMPLETED);
            session.setCompletedAt(now);
            session.setUpdatedAt(now);
            sessionRepository.save(session);
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

    private boolean isTerminalTask(CollaborationTaskEntity task) {
        return task.getStatus() == CollaborationTaskStatus.COMPLETED
                || task.getStatus() == CollaborationTaskStatus.REJECTED
                || task.getStatus() == CollaborationTaskStatus.CANCELLED;
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
            return requireParticipant(session, participantId);
        }
        if (task.getParticipantId() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "COLLABORATION_INVALID_REQUEST", "Task has no assigned participant");
        }
        return requireParticipant(session, task.getParticipantId());
    }

    private void clearParticipantCurrentTask(CollaborationSessionEntity session, UUID participantId, Instant now) {
        if (participantId == null) {
            return;
        }
        participantRepository
                .findByIdAndOrganizationId(participantId, session.getOrganizationId())
                .ifPresent(participant -> {
                    participant.setCurrentTaskId(null);
                    participant.setStatus(CollaborationParticipantStatus.IDLE);
                    participant.setUpdatedAt(now);
                    participantRepository.save(participant);
                });
    }

    private CollaborationParticipantEntity requireParticipant(CollaborationSessionEntity session, UUID participantId) {
        CollaborationParticipantEntity participant = participantRepository
                .findByIdAndOrganizationId(participantId, session.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_PARTICIPANT_NOT_FOUND", "Participant not found"));
        if (!participant.getSessionId().equals(session.getId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "COLLABORATION_PARTICIPANT_MISMATCH", "Participant does not belong to session");
        }
        return participant;
    }

    private CollaborationTaskEntity requireTask(CollaborationSessionEntity session, UUID taskId) {
        CollaborationTaskEntity task = taskRepository
                .findByIdAndOrganizationId(taskId, session.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_TASK_NOT_FOUND", "Task not found"));
        if (!task.getSessionId().equals(session.getId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "COLLABORATION_TASK_MISMATCH", "Task does not belong to session");
        }
        return task;
    }

    private void requireActiveSession(CollaborationSessionEntity session) {
        if (session.getStatus() == CollaborationSessionStatus.CANCELLED
                || session.getStatus() == CollaborationSessionStatus.COMPLETED
                || session.getStatus() == CollaborationSessionStatus.FAILED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_SESSION_INVALID_STATUS",
                    "Session is not active");
        }
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
