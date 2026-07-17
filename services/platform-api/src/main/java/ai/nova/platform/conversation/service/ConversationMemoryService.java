package ai.nova.platform.conversation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.conversation.entity.Conversation;
import ai.nova.platform.conversation.entity.ConversationMessage;
import ai.nova.platform.conversation.entity.ConversationMessageRole;
import ai.nova.platform.conversation.repository.ConversationMessageRepository;
import ai.nova.platform.conversation.repository.ConversationRepository;
import ai.nova.platform.conversation.validation.ConversationProperties;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class ConversationMemoryService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationAuditWriteService auditWriteService;

    public ConversationMemoryService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ConversationAuditWriteService auditWriteService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auditWriteService = auditWriteService;
    }

    public record AssembledContext(
            List<RuntimeMessage> messages,
            int droppedMessageCount,
            int totalPriorMessages,
            boolean truncated) {
    }

    @Transactional(readOnly = true)
    public AssembledContext assemble(
            UUID conversationId,
            UUID projectId,
            UUID organizationId,
            String currentUserMessage,
            ConversationProperties props,
            AuthenticatedUser user) {
        Conversation conversation = conversationRepository
                .findByIdAndProjectIdAndOrganizationId(conversationId, projectId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found"));

        validateMessageLength(currentUserMessage, props);

        List<ConversationMessage> priorDesc = messageRepository
                .findByConversationIdAndOrganizationIdAndProjectIdOrderBySequenceNumberDesc(
                        conversationId, organizationId, projectId);

        List<ConversationMessage> eligible = priorDesc.stream()
                .filter(m -> props.isStoreSystemMessage() || m.getRole() != ConversationMessageRole.SYSTEM)
                .toList();

        int totalPrior = eligible.size();
        int maxMessages = props.getMaxContextMessages();
        int maxCharacters = props.getMaxContextCharacters();
        int currentLength = currentUserMessage.length();

        List<ConversationMessage> selected = new ArrayList<>();
        int messageCount = 1;
        int characterCount = currentLength;

        for (ConversationMessage message : eligible) {
            int nextMessageCount = messageCount + 1;
            int nextCharacterCount = characterCount + message.getContent().length();
            if (nextMessageCount > maxMessages || nextCharacterCount > maxCharacters) {
                break;
            }
            selected.add(message);
            messageCount = nextMessageCount;
            characterCount = nextCharacterCount;
        }

        Collections.reverse(selected);

        List<RuntimeMessage> runtimeMessages = new ArrayList<>(selected.size() + 1);
        for (ConversationMessage message : selected) {
            runtimeMessages.add(toRuntimeMessage(message));
        }
        runtimeMessages.add(new RuntimeMessage("USER", currentUserMessage));

        int droppedCount = totalPrior - selected.size();
        boolean truncated = droppedCount > 0;

        if (truncated) {
            auditWriteService.writeMemoryTruncated(
                    conversation.getId(),
                    conversation.getOrganizationId(),
                    conversation.getProjectId(),
                    droppedCount,
                    totalPrior,
                    selected.size(),
                    user.getUserId());
        }

        return new AssembledContext(runtimeMessages, droppedCount, totalPrior, truncated);
    }

    private static RuntimeMessage toRuntimeMessage(ConversationMessage message) {
        return new RuntimeMessage(message.getRole().name(), message.getContent());
    }

    static void validateMessageLength(String content, ConversationProperties props) {
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Message must not be blank");
        }
        if (content.length() > props.getMaxMessageCharacters()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "CONVERSATION_MESSAGE_TOO_LONG",
                    "Message exceeds maximum length of " + props.getMaxMessageCharacters());
        }
    }
}
