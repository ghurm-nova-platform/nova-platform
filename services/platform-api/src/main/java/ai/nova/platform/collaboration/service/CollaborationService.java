package ai.nova.platform.collaboration.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.collaboration.config.CollaborationProperties;
import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.CollaborationConfigResponse;
import ai.nova.platform.collaboration.dto.CollaborationDtos.CreateSessionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.DecisionView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.InitialTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.MessageView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.ParticipantView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.RecordDecisionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SendMessageRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionSummary;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SharedContext;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TimelineEventView;
import ai.nova.platform.collaboration.entity.CollaborationDecisionEntity;
import ai.nova.platform.collaboration.entity.CollaborationDecisionType;
import ai.nova.platform.collaboration.entity.CollaborationMessageEntity;
import ai.nova.platform.collaboration.entity.CollaborationParticipantEntity;
import ai.nova.platform.collaboration.entity.CollaborationParticipantRole;
import ai.nova.platform.collaboration.entity.CollaborationParticipantStatus;
import ai.nova.platform.collaboration.entity.CollaborationSessionEntity;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskEntity;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventType;
import ai.nova.platform.collaboration.repository.CollaborationDecisionRepository;
import ai.nova.platform.collaboration.repository.CollaborationMessageRepository;
import ai.nova.platform.collaboration.repository.CollaborationParticipantRepository;
import ai.nova.platform.collaboration.repository.CollaborationSessionRepository;
import ai.nova.platform.collaboration.repository.CollaborationTaskRepository;
import ai.nova.platform.collaboration.security.CollaborationAuthorizationService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class CollaborationService {

    private final CollaborationProperties properties;
    private final CollaborationAuthorizationService authorizationService;
    private final CollaborationSessionService sessionService;
    private final CollaborationCoordinator coordinator;
    private final CollaborationTimelineService timelineService;
    private final CollaborationSessionRepository sessionRepository;
    private final CollaborationParticipantRepository participantRepository;
    private final CollaborationTaskRepository taskRepository;
    private final CollaborationMessageRepository messageRepository;
    private final CollaborationDecisionRepository decisionRepository;
    private final ProjectRepository projectRepository;
    private final AgentOrchestrationRunRepository orchestrationRunRepository;
    private final AuditRecordingSupport auditRecordingSupport;
    private final ObjectMapper objectMapper;

    public CollaborationService(
            CollaborationProperties properties,
            CollaborationAuthorizationService authorizationService,
            CollaborationSessionService sessionService,
            CollaborationCoordinator coordinator,
            CollaborationTimelineService timelineService,
            CollaborationSessionRepository sessionRepository,
            CollaborationParticipantRepository participantRepository,
            CollaborationTaskRepository taskRepository,
            CollaborationMessageRepository messageRepository,
            CollaborationDecisionRepository decisionRepository,
            ProjectRepository projectRepository,
            AgentOrchestrationRunRepository orchestrationRunRepository,
            AuditRecordingSupport auditRecordingSupport,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.sessionService = sessionService;
        this.coordinator = coordinator;
        this.timelineService = timelineService;
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.taskRepository = taskRepository;
        this.messageRepository = messageRepository;
        this.decisionRepository = decisionRepository;
        this.projectRepository = projectRepository;
        this.orchestrationRunRepository = orchestrationRunRepository;
        this.auditRecordingSupport = auditRecordingSupport;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        validateProjectScope(user, projectId);
        return sessionService.listSummaries(user.getOrganizationId(), projectId);
    }

    @Transactional(readOnly = true)
    public SessionDetail get(UUID sessionId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        sessionService.requireSession(sessionId, user.getOrganizationId());
        return sessionService.loadDetail(sessionId, user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<TimelineEventView> timeline(UUID sessionId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        return sessionService.loadTimeline(sessionId, user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<ParticipantView> participants(UUID sessionId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        return sessionService.loadParticipants(sessionId, user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<MessageView> messages(UUID sessionId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        return sessionService.loadMessages(sessionId, user.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public CollaborationConfigResponse config(AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return new CollaborationConfigResponse(
                properties.isEnabled(), properties.getPollingSeconds(), properties.getMaxMessages());
    }

    @Transactional
    public SessionDetail create(CreateSessionRequest request, AuthenticatedUser user) {
        authorizationService.requireWrite(user);
        requireEnabled();
        validateProject(user, request.projectId());

        if (request.orchestrationRunId() != null) {
            orchestrationRunRepository
                    .findByIdAndOrganizationIdAndProjectId(
                            request.orchestrationRunId(), user.getOrganizationId(), request.projectId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "ORCHESTRATION_RUN_NOT_FOUND", "Orchestration run not found"));
        }

        Instant now = Instant.now();
        SharedContext sharedContext = mergeSharedContext(request.sharedContext(), request.projectId());
        CollaborationSessionEntity session = new CollaborationSessionEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                request.projectId(),
                request.orchestrationRunId(),
                request.name(),
                CollaborationSessionStatus.CREATED,
                sessionService.serializeSharedContext(sharedContext),
                user.getUserId(),
                now);
        sessionRepository.save(session);

        List<CollaborationParticipantRole> roles = request.participantRoles() == null
                ? List.of()
                : request.participantRoles();
        for (CollaborationParticipantRole role : roles) {
            participantRepository.save(new CollaborationParticipantEntity(
                    UUID.randomUUID(),
                    session.getId(),
                    session.getOrganizationId(),
                    role,
                    CollaborationParticipantStatus.IDLE,
                    now));
        }

        if (request.initialTasks() != null) {
            for (InitialTaskRequest initialTask : request.initialTasks()) {
                taskRepository.save(new CollaborationTaskEntity(
                        UUID.randomUUID(),
                        session.getId(),
                        session.getOrganizationId(),
                        initialTask.taskKey(),
                        initialTask.title(),
                        CollaborationTaskStatus.PENDING,
                        initialTask.dependsOnTaskId(),
                        initialTask.artifactRef(),
                        initialTask.parallelGroup(),
                        now));
            }
        }

        timelineService.append(
                session.getId(),
                session.getOrganizationId(),
                CollaborationTimelineEventType.CREATED,
                "Collaboration session created",
                null,
                null,
                null,
                null,
                Map.of("name", session.getName()));

        audit(user, session, AuditAction.CREATE, AuditResult.SUCCESS, Map.of("name", session.getName()));
        return sessionService.loadDetail(session.getId(), session.getOrganizationId());
    }

    @Transactional
    public SessionDetail assign(UUID sessionId, AssignTaskRequest request, AuthenticatedUser user) {
        authorizationService.requireWrite(user);
        requireEnabled();
        CollaborationSessionEntity session = sessionService.requireSession(sessionId, user.getOrganizationId());
        coordinator.handleTaskAction(session, request);
        audit(user, session, AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("taskId", request.taskId().toString()));
        return sessionService.loadDetail(sessionId, user.getOrganizationId());
    }

    @Transactional
    public SessionDetail sendMessage(UUID sessionId, SendMessageRequest request, AuthenticatedUser user) {
        authorizationService.requireWrite(user);
        requireEnabled();
        CollaborationSessionEntity session = sessionService.requireSession(sessionId, user.getOrganizationId());
        requireWritableSession(session);

        if (request.taskId() != null) {
            taskRepository
                    .findByIdAndOrganizationId(request.taskId(), session.getOrganizationId())
                    .filter(task -> task.getSessionId().equals(sessionId))
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "COLLABORATION_TASK_NOT_FOUND", "Task not found"));
        }

        Instant now = Instant.now();
        CollaborationMessageEntity message = new CollaborationMessageEntity(
                UUID.randomUUID(),
                sessionId,
                session.getOrganizationId(),
                request.senderRole(),
                request.messageType(),
                request.content(),
                request.taskId(),
                now);
        messageRepository.save(message);

        timelineService.append(
                sessionId,
                session.getOrganizationId(),
                CollaborationTimelineEventType.MESSAGE_SENT,
                "Message sent",
                request.senderRole(),
                request.taskId(),
                message.getId(),
                null,
                Map.of("messageType", request.messageType().name()));

        audit(user, session, AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("messageType", request.messageType().name()));
        return sessionService.loadDetail(sessionId, user.getOrganizationId());
    }

    @Transactional
    public SessionDetail recordDecision(UUID sessionId, RecordDecisionRequest request, AuthenticatedUser user) {
        authorizationService.requireWrite(user);
        requireEnabled();
        CollaborationSessionEntity session = sessionService.requireSession(sessionId, user.getOrganizationId());

        Instant now = Instant.now();
        CollaborationDecisionEntity decision = new CollaborationDecisionEntity(
                UUID.randomUUID(),
                sessionId,
                session.getOrganizationId(),
                request.decisionType(),
                request.summary(),
                serializeDetails(request.details()),
                user.getUserId(),
                request.taskId(),
                now);
        decisionRepository.save(decision);

        applyDecision(session, request);

        CollaborationTimelineEventType timelineType = resolveTimelineType(request.decisionType());
        timelineService.append(
                sessionId,
                session.getOrganizationId(),
                timelineType,
                request.summary(),
                CollaborationParticipantRole.HUMAN_REVIEWER,
                request.taskId(),
                null,
                decision.getId(),
                request.details() == null ? Map.of() : request.details());

        audit(
                user,
                session,
                resolveAuditAction(request.decisionType()),
                AuditResult.SUCCESS,
                Map.of("decisionType", request.decisionType().name()));
        return sessionService.loadDetail(sessionId, user.getOrganizationId());
    }

    @Transactional
    public SessionDetail pause(UUID sessionId, AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        requireEnabled();
        CollaborationSessionEntity session = sessionService.requireSession(sessionId, user.getOrganizationId());
        coordinator.pauseSession(session);
        audit(user, session, AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("action", "pause"));
        return sessionService.loadDetail(sessionId, user.getOrganizationId());
    }

    @Transactional
    public SessionDetail resume(UUID sessionId, AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        requireEnabled();
        CollaborationSessionEntity session = sessionService.requireSession(sessionId, user.getOrganizationId());
        coordinator.resumeSession(session);
        audit(user, session, AuditAction.UPDATE, AuditResult.SUCCESS, Map.of("action", "resume"));
        return sessionService.loadDetail(sessionId, user.getOrganizationId());
    }

    @Transactional
    public SessionDetail cancel(UUID sessionId, AuthenticatedUser user) {
        authorizationService.requireAdmin(user);
        requireEnabled();
        CollaborationSessionEntity session = sessionService.requireSession(sessionId, user.getOrganizationId());
        coordinator.cancelSession(session);
        audit(user, session, AuditAction.CANCEL, AuditResult.SUCCESS, Map.of());
        return sessionService.loadDetail(sessionId, user.getOrganizationId());
    }

    private void applyDecision(CollaborationSessionEntity session, RecordDecisionRequest request) {
        switch (request.decisionType()) {
            case APPROVE -> {
                if (request.taskId() != null) {
                    CollaborationTaskEntity task = requireTask(session, request.taskId());
                    coordinator.completeTask(session, task, null);
                }
            }
            case REJECT -> {
                if (request.taskId() != null) {
                    CollaborationTaskEntity task = requireTask(session, request.taskId());
                    coordinator.rejectTask(session, task, request.summary());
                }
            }
            case RESOLVE_CONFLICT -> {
                session.setConflictDetected(false);
                session.setConflictDetailsJson(null);
                session.setStatus(CollaborationSessionStatus.ACTIVE);
                session.setUpdatedAt(Instant.now());
                sessionRepository.save(session);
            }
            case PAUSE -> coordinator.pauseSession(session);
            case RESUME -> coordinator.resumeSession(session);
            case CANCEL -> coordinator.cancelSession(session);
            case REQUEST_REVIEW, REQUEST_APPROVAL, REQUEST_CLARIFICATION -> {
                // Recorded as decisions and timeline entries only.
            }
        }
    }

    private CollaborationTaskEntity requireTask(CollaborationSessionEntity session, UUID taskId) {
        return taskRepository
                .findByIdAndOrganizationId(taskId, session.getOrganizationId())
                .filter(task -> task.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_TASK_NOT_FOUND", "Task not found"));
    }

    private CollaborationTimelineEventType resolveTimelineType(CollaborationDecisionType decisionType) {
        return switch (decisionType) {
            case APPROVE -> CollaborationTimelineEventType.APPROVAL;
            case RESOLVE_CONFLICT -> CollaborationTimelineEventType.CONFLICT;
            case PAUSE -> CollaborationTimelineEventType.PAUSED;
            case RESUME -> CollaborationTimelineEventType.RESUMED;
            case CANCEL -> CollaborationTimelineEventType.CANCELLED;
            default -> CollaborationTimelineEventType.DECISION;
        };
    }

    private AuditAction resolveAuditAction(CollaborationDecisionType decisionType) {
        return switch (decisionType) {
            case APPROVE -> AuditAction.APPROVE;
            case REJECT -> AuditAction.REJECT;
            case CANCEL -> AuditAction.CANCEL;
            default -> AuditAction.UPDATE;
        };
    }

    private SharedContext mergeSharedContext(SharedContext sharedContext, UUID projectId) {
        if (sharedContext == null) {
            return new SharedContext(projectId, null, null, null, null, null, List.of());
        }
        UUID resolvedProjectId = sharedContext.projectId() == null ? projectId : sharedContext.projectId();
        if (!resolvedProjectId.equals(projectId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "COLLABORATION_INVALID_REQUEST", "Shared context project mismatch");
        }
        return new SharedContext(
                resolvedProjectId,
                sharedContext.repositoryId(),
                sharedContext.branch(),
                sharedContext.releaseId(),
                sharedContext.environmentId(),
                sharedContext.executionId(),
                sharedContext.auditEventIds() == null ? List.of() : sharedContext.auditEventIds());
    }

    private void validateProject(AuthenticatedUser user, UUID projectId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void validateProjectScope(AuthenticatedUser user, UUID projectId) {
        if (projectId == null) {
            return;
        }
        validateProject(user, projectId);
    }

    private void requireWritableSession(CollaborationSessionEntity session) {
        if (session.getStatus() == CollaborationSessionStatus.CANCELLED
                || session.getStatus() == CollaborationSessionStatus.COMPLETED
                || session.getStatus() == CollaborationSessionStatus.FAILED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "COLLABORATION_SESSION_INVALID_STATUS",
                    "Session is not accepting messages");
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "COLLABORATION_DISABLED", "Collaboration is disabled");
        }
    }

    private String serializeDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void audit(
            AuthenticatedUser user,
            CollaborationSessionEntity session,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        auditRecordingSupport.recordDomainEvent(
                user,
                session.getProjectId(),
                AuditEntityType.CONFIGURATION,
                session.getId(),
                session.getName(),
                action,
                result,
                AuditSource.COLLABORATION,
                details);
    }
}
