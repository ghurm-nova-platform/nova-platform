package ai.nova.platform.conversation.mapper;

import org.springframework.stereotype.Component;

import ai.nova.platform.conversation.dto.ConversationDtos.ConversationMessageResponse;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationResponse;
import ai.nova.platform.conversation.entity.Conversation;
import ai.nova.platform.conversation.entity.ConversationMessage;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getProjectId(),
                conversation.getAgentId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getMessageCount(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getVersion());
    }

    public ConversationMessageResponse toMessageResponse(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getSequenceNumber(),
                message.getExecutionId(),
                message.getCreatedAt());
    }
}
