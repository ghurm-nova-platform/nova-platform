package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.llm.dto.LlmDtos.AppendMessageRequest;
import ai.nova.platform.llm.dto.LlmDtos.ConversationView;
import ai.nova.platform.llm.dto.LlmDtos.CreateConversationRequest;
import ai.nova.platform.llm.dto.LlmDtos.MessageView;
import ai.nova.platform.llm.entity.LlmConversationEntity;
import ai.nova.platform.llm.entity.LlmConversationStatus;
import ai.nova.platform.llm.entity.LlmMessageEntity;
import ai.nova.platform.llm.entity.LlmMessageRole;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.mapper.LlmMapper;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.llm.repository.LlmConversationRepository;
import ai.nova.platform.llm.repository.LlmMessageRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class LlmConversationService {

    private final LlmConversationRepository conversationRepository;
    private final LlmMessageRepository messageRepository;
    private final LlmMapper mapper;
    private final LlmAuditService auditService;

    public LlmConversationService(
            LlmConversationRepository conversationRepository,
            LlmMessageRepository messageRepository,
            LlmMapper mapper,
            LlmAuditService auditService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public ConversationView create(CreateConversationRequest request, AuthenticatedUser user) {
        Instant now = Instant.now();
        LlmConversationEntity entity = new LlmConversationEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                user.getUserId(),
                request.modelId(),
                request.title() == null ? "Conversation" : request.title(),
                now);
        entity.setProjectId(request.projectId());
        conversationRepository.save(entity);
        auditService.record(
                user, entity.getId(), entity.getTitle(), AuditAction.CREATE, AuditResult.SUCCESS, Map.of());
        return mapper.toConversationView(entity);
    }

    @Transactional(readOnly = true)
    public List<ConversationView> list(AuthenticatedUser user) {
        return conversationRepository
                .findByOrganizationIdAndUserIdOrderByUpdatedAtDesc(user.getOrganizationId(), user.getUserId())
                .stream()
                .filter(c -> c.getStatus() != LlmConversationStatus.DELETED)
                .map(mapper::toConversationView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationView get(UUID conversationId, AuthenticatedUser user) {
        return mapper.toConversationView(require(conversationId, user));
    }

    @Transactional
    public MessageView append(UUID conversationId, AppendMessageRequest request, AuthenticatedUser user) {
        LlmConversationEntity conversation = require(conversationId, user);
        int nextSeq = messageRepository
                .findTopByConversationIdOrderBySequenceNoDesc(conversationId)
                .map(m -> m.getSequenceNo() + 1)
                .orElse(1);
        LlmMessageEntity message = new LlmMessageEntity(
                UUID.randomUUID(),
                conversationId,
                request.role(),
                request.content(),
                nextSeq,
                Instant.now());
        messageRepository.save(message);
        conversation.touch(Instant.now());
        return mapper.toMessageView(message);
    }

    @Transactional
    public void appendAssistantReply(UUID conversationId, AuthenticatedUser user, LlmCompletionResult result) {
        LlmConversationEntity conversation = require(conversationId, user);
        int nextSeq = messageRepository
                .findTopByConversationIdOrderBySequenceNoDesc(conversationId)
                .map(m -> m.getSequenceNo() + 1)
                .orElse(1);
        LlmMessageEntity message = new LlmMessageEntity(
                UUID.randomUUID(),
                conversationId,
                LlmMessageRole.ASSISTANT,
                result.content(),
                nextSeq,
                Instant.now());
        message.setTokenCount(result.outputTokens());
        messageRepository.save(message);
        conversation.setTokenUsageInput(conversation.getTokenUsageInput() + result.inputTokens());
        conversation.setTokenUsageOutput(conversation.getTokenUsageOutput() + result.outputTokens());
        conversation.touch(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<MessageView> history(UUID conversationId, AuthenticatedUser user) {
        require(conversationId, user);
        return messageRepository.findByConversationIdOrderBySequenceNoAsc(conversationId).stream()
                .map(mapper::toMessageView)
                .toList();
    }

    @Transactional
    public ConversationView summarize(UUID conversationId, AuthenticatedUser user) {
        LlmConversationEntity conversation = require(conversationId, user);
        List<LlmMessageEntity> messages =
                messageRepository.findByConversationIdOrderBySequenceNoAsc(conversationId);
        String summary = messages.isEmpty()
                ? "Empty conversation"
                : "Summary: " + messages.size() + " messages; last: "
                        + truncate(messages.get(messages.size() - 1).getContent(), 120);
        conversation.setSummary(summary);
        conversation.touch(Instant.now());
        return mapper.toConversationView(conversation);
    }

    private LlmConversationEntity require(UUID conversationId, AuthenticatedUser user) {
        LlmConversationEntity entity = conversationRepository
                .findByIdAndOrganizationId(conversationId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, LlmErrorCodes.NOT_FOUND, "Conversation not found"));
        if (entity.getStatus() == LlmConversationStatus.DELETED) {
            throw new ApiException(HttpStatus.NOT_FOUND, LlmErrorCodes.NOT_FOUND, "Conversation not found");
        }
        return entity;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
