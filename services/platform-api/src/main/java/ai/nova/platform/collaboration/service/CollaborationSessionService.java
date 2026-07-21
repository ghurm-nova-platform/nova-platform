package ai.nova.platform.collaboration.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.collaboration.config.CollaborationProperties;
import ai.nova.platform.collaboration.dto.CollaborationDtos.DecisionView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.MessageView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.ParticipantView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionSummary;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SharedContext;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TimelineEventView;
import ai.nova.platform.collaboration.entity.CollaborationDecisionEntity;
import ai.nova.platform.collaboration.entity.CollaborationMessageEntity;
import ai.nova.platform.collaboration.entity.CollaborationParticipantEntity;
import ai.nova.platform.collaboration.entity.CollaborationSessionEntity;
import ai.nova.platform.collaboration.entity.CollaborationTaskEntity;
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventEntity;
import ai.nova.platform.collaboration.repository.CollaborationDecisionRepository;
import ai.nova.platform.collaboration.repository.CollaborationMessageRepository;
import ai.nova.platform.collaboration.repository.CollaborationParticipantRepository;
import ai.nova.platform.collaboration.repository.CollaborationSessionRepository;
import ai.nova.platform.collaboration.repository.CollaborationTaskRepository;
import ai.nova.platform.collaboration.repository.CollaborationTimelineEventRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class CollaborationSessionService {

    private final CollaborationProperties properties;
    private final CollaborationSessionRepository sessionRepository;
    private final CollaborationParticipantRepository participantRepository;
    private final CollaborationTaskRepository taskRepository;
    private final CollaborationMessageRepository messageRepository;
    private final CollaborationDecisionRepository decisionRepository;
    private final CollaborationTimelineEventRepository timelineEventRepository;
    private final CollaborationTimelineService timelineService;
    private final ObjectMapper objectMapper;

    public CollaborationSessionService(
            CollaborationProperties properties,
            CollaborationSessionRepository sessionRepository,
            CollaborationParticipantRepository participantRepository,
            CollaborationTaskRepository taskRepository,
            CollaborationMessageRepository messageRepository,
            CollaborationDecisionRepository decisionRepository,
            CollaborationTimelineEventRepository timelineEventRepository,
            CollaborationTimelineService timelineService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.taskRepository = taskRepository;
        this.messageRepository = messageRepository;
        this.decisionRepository = decisionRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.timelineService = timelineService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> listSummaries(UUID organizationId, UUID projectId) {
        List<CollaborationSessionEntity> sessions = projectId == null
                ? sessionRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                : sessionRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        organizationId, projectId);
        return sessions.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public SessionDetail loadDetail(UUID sessionId, UUID organizationId) {
        CollaborationSessionEntity session = requireSession(sessionId, organizationId);
        return toDetail(session, true, true, true, true, true);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventView> loadTimeline(UUID sessionId, UUID organizationId) {
        requireSession(sessionId, organizationId);
        return timelineEventRepository
                .findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(sessionId, organizationId)
                .stream()
                .map(this::toTimelineView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantView> loadParticipants(UUID sessionId, UUID organizationId) {
        requireSession(sessionId, organizationId);
        return participantRepository
                .findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(sessionId, organizationId)
                .stream()
                .map(this::toParticipantView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageView> loadMessages(UUID sessionId, UUID organizationId) {
        requireSession(sessionId, organizationId);
        int limit = Math.max(properties.getMaxMessages(), 1);
        return messageRepository
                .findBySessionIdAndOrganizationIdOrderByCreatedAtDesc(
                        sessionId, organizationId, PageRequest.of(0, limit))
                .stream()
                .map(this::toMessageView)
                .toList();
    }

    public CollaborationSessionEntity requireSession(UUID sessionId, UUID organizationId) {
        return sessionRepository
                .findByIdAndOrganizationId(sessionId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_SESSION_NOT_FOUND", "Collaboration session not found"));
    }

    public SessionDetail toDetail(
            CollaborationSessionEntity session,
            boolean includeParticipants,
            boolean includeTasks,
            boolean includeMessages,
            boolean includeDecisions,
            boolean includeTimeline) {
        UUID sessionId = session.getId();
        UUID organizationId = session.getOrganizationId();

        List<ParticipantView> participants = includeParticipants
                ? participantRepository
                        .findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(sessionId, organizationId)
                        .stream()
                        .map(this::toParticipantView)
                        .toList()
                : List.of();

        List<TaskView> tasks = includeTasks
                ? taskRepository
                        .findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(sessionId, organizationId)
                        .stream()
                        .map(this::toTaskView)
                        .toList()
                : List.of();

        List<MessageView> messages = includeMessages
                ? messageRepository
                        .findBySessionIdAndOrganizationIdOrderByCreatedAtDesc(
                                sessionId,
                                organizationId,
                                PageRequest.of(0, Math.max(properties.getMaxMessages(), 1)))
                        .stream()
                        .map(this::toMessageView)
                        .toList()
                : List.of();

        List<DecisionView> decisions = includeDecisions
                ? decisionRepository
                        .findBySessionIdAndOrganizationIdOrderByCreatedAtDesc(sessionId, organizationId)
                        .stream()
                        .map(this::toDecisionView)
                        .toList()
                : List.of();

        List<TimelineEventView> timeline = includeTimeline
                ? timelineEventRepository
                        .findBySessionIdAndOrganizationIdOrderByCreatedAtAsc(sessionId, organizationId)
                        .stream()
                        .map(this::toTimelineView)
                        .toList()
                : List.of();

        return new SessionDetail(
                session.getId(),
                session.getOrganizationId(),
                session.getProjectId(),
                session.getOrchestrationRunId(),
                session.getName(),
                session.getStatus(),
                parseSharedContext(session.getSharedContextJson()),
                session.getParallelGroup(),
                session.isConflictDetected(),
                timelineService.parseDetails(session.getConflictDetailsJson()),
                session.getCreatedBy(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                participants,
                tasks,
                messages,
                decisions,
                timeline);
    }

    public SessionSummary toSummary(CollaborationSessionEntity session) {
        return new SessionSummary(
                session.getId(),
                session.getOrganizationId(),
                session.getProjectId(),
                session.getOrchestrationRunId(),
                session.getName(),
                session.getStatus(),
                session.isConflictDetected(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    public SharedContext parseSharedContext(String sharedContextJson) {
        if (sharedContextJson == null || sharedContextJson.isBlank()) {
            return new SharedContext(null, null, null, null, null, null, List.of());
        }
        try {
            return objectMapper.readValue(sharedContextJson, SharedContext.class);
        } catch (JsonProcessingException ex) {
            return new SharedContext(null, null, null, null, null, null, List.of());
        }
    }

    public String serializeSharedContext(SharedContext sharedContext) {
        SharedContext context = sharedContext == null
                ? new SharedContext(null, null, null, null, null, null, List.of())
                : sharedContext;
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private ParticipantView toParticipantView(CollaborationParticipantEntity participant) {
        return new ParticipantView(
                participant.getId(),
                participant.getParticipantRole(),
                participant.getStatus(),
                participant.getCurrentTaskId(),
                participant.getProgressPercent(),
                participant.getParallelGroup(),
                participant.getStartedAt(),
                participant.getCompletedAt(),
                participant.getCreatedAt());
    }

    private TaskView toTaskView(CollaborationTaskEntity task) {
        return new TaskView(
                task.getId(),
                task.getTaskKey(),
                task.getTitle(),
                task.getStatus(),
                task.getParticipantId(),
                task.getDependsOnTaskId(),
                task.getBlockedByTaskId(),
                task.getCompletedByParticipantId(),
                task.getArtifactRef(),
                task.getParallelGroup(),
                task.getAssignedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getCreatedAt());
    }

    private MessageView toMessageView(CollaborationMessageEntity message) {
        return new MessageView(
                message.getId(),
                message.getSenderRole(),
                message.getMessageType(),
                message.getContent(),
                message.getTaskId(),
                message.getCreatedAt());
    }

    private DecisionView toDecisionView(CollaborationDecisionEntity decision) {
        return new DecisionView(
                decision.getId(),
                decision.getDecisionType(),
                decision.getSummary(),
                timelineService.parseDetails(decision.getDetailsJson()),
                decision.getDecidedBy(),
                decision.getTaskId(),
                decision.getCreatedAt());
    }

    private TimelineEventView toTimelineView(CollaborationTimelineEventEntity event) {
        return new TimelineEventView(
                event.getId(),
                event.getEventType(),
                event.getSummary(),
                event.getActorRole(),
                event.getTaskId(),
                event.getMessageId(),
                event.getDecisionId(),
                timelineService.parseDetails(event.getDetailsJson()),
                event.getCreatedAt());
    }
}
