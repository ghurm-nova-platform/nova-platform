package ai.nova.platform.conversation.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationCreateRequest;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationMessageCreateRequest;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationMessageResponse;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationResponse;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationUpdateRequest;
import ai.nova.platform.conversation.entity.Conversation;
import ai.nova.platform.conversation.entity.ConversationAuditAction;
import ai.nova.platform.conversation.entity.ConversationAuditLog;
import ai.nova.platform.conversation.entity.ConversationExecutionRequest;
import ai.nova.platform.conversation.entity.ConversationMessage;
import ai.nova.platform.conversation.entity.ConversationMessageRole;
import ai.nova.platform.conversation.entity.ConversationStatus;
import ai.nova.platform.conversation.mapper.ConversationMapper;
import ai.nova.platform.conversation.repository.ConversationAuditLogRepository;
import ai.nova.platform.conversation.repository.ConversationExecutionRequestRepository;
import ai.nova.platform.conversation.repository.ConversationMessageRepository;
import ai.nova.platform.conversation.repository.ConversationRepository;
import ai.nova.platform.conversation.security.ConversationAuthorizationService;
import ai.nova.platform.conversation.validation.ConversationProperties;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.correlation.CorrelationIdFilter;
import ai.nova.platform.web.error.ApiException;

@Service
public class ConversationService {

    public record ExecutionUserMessageResult(UUID userMessageId, UUID executionId, boolean duplicate) {
    }

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationAuditLogRepository auditLogRepository;
    private final ConversationExecutionRequestRepository executionRequestRepository;
    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final ConversationMapper conversationMapper;
    private final ConversationAuthorizationService authorizationService;
    private final ConversationProperties conversationProperties;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ConversationAuditLogRepository auditLogRepository,
            ConversationExecutionRequestRepository executionRequestRepository,
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            ConversationMapper conversationMapper,
            ConversationAuthorizationService authorizationService,
            ConversationProperties conversationProperties) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auditLogRepository = auditLogRepository;
        this.executionRequestRepository = executionRequestRepository;
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.conversationMapper = conversationMapper;
        this.authorizationService = authorizationService;
        this.conversationProperties = conversationProperties;
    }

    @Transactional
    public ConversationResponse create(UUID projectId, ConversationCreateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_CREATE);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        Agent agent = requireActiveAgent(projectId, request.agentId(), user.getOrganizationId());

        String title = normalizeTitle(request.title());
        validateTitleLength(title);

        Instant now = Instant.now();
        Conversation conversation = new Conversation(
                UUID.randomUUID(),
                user.getOrganizationId(),
                projectId,
                agent.getId(),
                title,
                ConversationStatus.ACTIVE,
                user.getUserId(),
                now);
        conversationRepository.save(conversation);
        writeAudit(conversation, ConversationAuditAction.CREATED, null, user.getUserId());
        return conversationMapper.toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> list(
            UUID projectId,
            AuthenticatedUser user,
            UUID agentId,
            ConversationStatus status,
            String search,
            Pageable pageable) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        return conversationRepository
                .search(user.getOrganizationId(), projectId, agentId, status, search, pageable)
                .map(conversationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(UUID projectId, UUID conversationId, AuthenticatedUser user) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        return conversationMapper.toResponse(requireConversation(projectId, conversationId, user.getOrganizationId()));
    }

    @Transactional
    public ConversationResponse update(
            UUID projectId, UUID conversationId, ConversationUpdateRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_UPDATE);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        Conversation conversation = requireConversation(projectId, conversationId, user.getOrganizationId());
        if (conversation.getStatus() == ConversationStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "CONVERSATION_ARCHIVED", "Conversation is archived");
        }
        assertVersion(conversation, request.version());

        String title = normalizeTitle(request.title());
        validateTitleLength(title);
        String oldTitle = conversation.getTitle();
        conversation.setTitle(title);
        conversation.setUpdatedBy(user.getUserId());
        conversation.setUpdatedAt(Instant.now());
        Conversation saved = saveWithOptimisticLock(conversation);
        writeAudit(saved, ConversationAuditAction.TITLE_UPDATED, oldTitle, user.getUserId());
        return conversationMapper.toResponse(saved);
    }

    @Transactional
    public ConversationResponse archive(UUID projectId, UUID conversationId, AuthenticatedUser user) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_ARCHIVE);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        Conversation conversation = requireConversation(projectId, conversationId, user.getOrganizationId());
        if (conversation.getStatus() == ConversationStatus.ARCHIVED) {
            return conversationMapper.toResponse(conversation);
        }
        conversation.setStatus(ConversationStatus.ARCHIVED);
        conversation.setUpdatedBy(user.getUserId());
        conversation.setUpdatedAt(Instant.now());
        Conversation saved = conversationRepository.save(conversation);
        writeAudit(saved, ConversationAuditAction.ARCHIVED, null, user.getUserId());
        return conversationMapper.toResponse(saved);
    }

    @Transactional
    public ConversationResponse restore(UUID projectId, UUID conversationId, AuthenticatedUser user) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_ARCHIVE);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        Conversation conversation = requireConversation(projectId, conversationId, user.getOrganizationId());
        if (conversation.getStatus() == ConversationStatus.ACTIVE) {
            return conversationMapper.toResponse(conversation);
        }
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setUpdatedBy(user.getUserId());
        conversation.setUpdatedAt(Instant.now());
        Conversation saved = conversationRepository.save(conversation);
        writeAudit(saved, ConversationAuditAction.RESTORED, null, user.getUserId());
        return conversationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ConversationMessageResponse> listMessages(
            UUID projectId, UUID conversationId, AuthenticatedUser user, Pageable pageable) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_MESSAGE_READ);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        requireConversation(projectId, conversationId, user.getOrganizationId());
        return messageRepository
                .findByConversationIdAndOrganizationIdAndProjectIdOrderBySequenceNumberAsc(
                        conversationId, user.getOrganizationId(), projectId, pageable)
                .map(conversationMapper::toMessageResponse);
    }

    @Transactional
    public ConversationMessageResponse addUserMessage(
            UUID projectId,
            UUID conversationId,
            ConversationMessageCreateRequest request,
            AuthenticatedUser user) {
        authorizationService.require(user, ConversationAuthorizationService.CONVERSATION_MESSAGE_CREATE);
        requireProjectInOrganization(projectId, user.getOrganizationId());
        Conversation conversation = requireConversation(projectId, conversationId, user.getOrganizationId());
        requireActiveConversation(conversation);

        String content = request.content().trim();
        ConversationMemoryService.validateMessageLength(content, conversationProperties);

        ConversationMessage message = appendMessage(
                conversation, ConversationMessageRole.USER, content, null, null, user.getUserId());
        return conversationMapper.toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findExistingExecutionId(UUID conversationId, UUID clientRequestId) {
        return executionRequestRepository
                .findByConversationIdAndClientRequestId(conversationId, clientRequestId)
                .map(ConversationExecutionRequest::getExecutionId);
    }

    @Transactional(readOnly = true)
    public Conversation requireActiveConversationForExecution(
            UUID projectId, UUID agentId, UUID conversationId, UUID organizationId) {
        Conversation conversation = requireConversation(projectId, conversationId, organizationId);
        if (conversation.getStatus() == ConversationStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "CONVERSATION_ARCHIVED", "Conversation is archived");
        }
        if (!conversation.getAgentId().equals(agentId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "CONVERSATION_AGENT_MISMATCH", "Conversation agent mismatch");
        }
        return conversation;
    }

    @Transactional
    public ExecutionUserMessageResult registerExecutionUserMessage(
            UUID projectId,
            UUID agentId,
            UUID conversationId,
            UUID executionId,
            UUID clientRequestId,
            String content,
            AuthenticatedUser user) {
        Optional<UUID> existingExecutionId = findExistingExecutionId(conversationId, clientRequestId);
        if (existingExecutionId.isPresent()) {
            return new ExecutionUserMessageResult(null, existingExecutionId.get(), true);
        }

        Conversation conversation = conversationRepository
                .findForUpdate(conversationId, projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found"));

        existingExecutionId = findExistingExecutionId(conversationId, clientRequestId);
        if (existingExecutionId.isPresent()) {
            return new ExecutionUserMessageResult(null, existingExecutionId.get(), true);
        }

        if (conversation.getStatus() == ConversationStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "CONVERSATION_ARCHIVED", "Conversation is archived");
        }
        if (!conversation.getAgentId().equals(agentId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "CONVERSATION_AGENT_MISMATCH", "Conversation agent mismatch");
        }

        ConversationMessage message = appendMessageLocked(
                conversation,
                ConversationMessageRole.USER,
                content,
                executionId,
                clientRequestId,
                user.getUserId());

        try {
            executionRequestRepository.saveAndFlush(new ConversationExecutionRequest(
                    UUID.randomUUID(),
                    conversationId,
                    conversation.getOrganizationId(),
                    projectId,
                    agentId,
                    clientRequestId,
                    executionId,
                    message.getId(),
                    Instant.now()));
        } catch (DataIntegrityViolationException ex) {
            Optional<UUID> duplicateExecutionId = findExistingExecutionId(conversationId, clientRequestId);
            if (duplicateExecutionId.isPresent()) {
                return new ExecutionUserMessageResult(null, duplicateExecutionId.get(), true);
            }
            throw ex;
        }

        return new ExecutionUserMessageResult(message.getId(), executionId, false);
    }

    @Transactional
    public void appendAssistantMessage(
            UUID projectId, UUID conversationId, UUID executionId, String content, AuthenticatedUser user) {
        Conversation conversation = conversationRepository
                .findForUpdate(conversationId, projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found"));
        appendMessageLocked(
                conversation,
                ConversationMessageRole.ASSISTANT,
                content,
                executionId,
                null,
                user.getUserId());
    }

    private ConversationMessage appendMessage(
            Conversation conversation,
            ConversationMessageRole role,
            String content,
            UUID executionId,
            UUID clientRequestId,
            UUID createdBy) {
        Conversation locked = conversationRepository
                .findForUpdate(conversation.getId(), conversation.getProjectId(), conversation.getOrganizationId())
                .orElseThrow();
        return appendMessageLocked(locked, role, content, executionId, clientRequestId, createdBy);
    }

    private ConversationMessage appendMessageLocked(
            Conversation conversation,
            ConversationMessageRole role,
            String content,
            UUID executionId,
            UUID clientRequestId,
            UUID createdBy) {
        Instant now = Instant.now();
        int nextSequence = conversation.getMessageCount() + 1;
        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID(),
                conversation.getId(),
                conversation.getOrganizationId(),
                conversation.getProjectId(),
                conversation.getAgentId(),
                executionId,
                role,
                content,
                nextSequence,
                createdBy,
                now,
                clientRequestId);
        messageRepository.save(message);

        conversation.setMessageCount(nextSequence);
        conversation.setLastMessageAt(now);
        conversation.setUpdatedBy(createdBy);
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        String metadata = String.format(
                "{\"role\":\"%s\",\"sequenceNumber\":%d,\"messageLength\":%d,\"executionId\":%s}",
                role.name(),
                nextSequence,
                content.length(),
                executionId != null ? "\"" + executionId + "\"" : "null");
        auditLogRepository.save(new ConversationAuditLog(
                UUID.randomUUID(),
                conversation.getId(),
                conversation.getOrganizationId(),
                conversation.getProjectId(),
                ConversationAuditAction.MESSAGE_ADDED,
                metadata,
                createdBy,
                now,
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));

        return message;
    }

    private Conversation requireConversation(UUID projectId, UUID conversationId, UUID organizationId) {
        return conversationRepository
                .findByIdAndProjectIdAndOrganizationId(conversationId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found"));
    }

    private void requireActiveConversation(Conversation conversation) {
        if (conversation.getStatus() == ConversationStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "CONVERSATION_ARCHIVED", "Conversation is archived");
        }
    }

    private Agent requireActiveAgent(UUID projectId, UUID agentId, UUID organizationId) {
        Agent agent = agentRepository
                .findByIdAndProjectIdAndOrganizationId(agentId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
        if (agent.getStatus() == AgentStatus.ARCHIVED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "AGENT_ARCHIVED", "Agent is archived");
        }
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "AGENT_NOT_ACTIVE", "Agent is not active");
        }
        return agent;
    }

    private Project requireProjectInOrganization(UUID projectId, UUID organizationId) {
        return projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void assertVersion(Conversation conversation, Integer version) {
        if (version == null || !version.equals(conversation.getVersion())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CONVERSATION_VERSION_CONFLICT",
                    "Conversation was modified by another request");
        }
    }

    private Conversation saveWithOptimisticLock(Conversation conversation) {
        try {
            return conversationRepository.saveAndFlush(conversation);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CONVERSATION_VERSION_CONFLICT",
                    "Conversation was modified by another request");
        }
    }

    private static String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        return title.trim();
    }

    private void validateTitleLength(String title) {
        if (title != null && title.length() > conversationProperties.getMaxTitleLength()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INPUT",
                    "Title exceeds maximum length of " + conversationProperties.getMaxTitleLength());
        }
    }

    private void writeAudit(
            Conversation conversation, ConversationAuditAction action, String oldTitle, UUID performedBy) {
        String metadata = oldTitle != null
                ? String.format("{\"oldTitleLength\":%d}", oldTitle.length())
                : null;
        auditLogRepository.save(new ConversationAuditLog(
                UUID.randomUUID(),
                conversation.getId(),
                conversation.getOrganizationId(),
                conversation.getProjectId(),
                action,
                metadata,
                performedBy,
                Instant.now(),
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));
    }
}
