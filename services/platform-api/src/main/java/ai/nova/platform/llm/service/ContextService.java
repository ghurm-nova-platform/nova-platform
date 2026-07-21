package ai.nova.platform.llm.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import ai.nova.platform.llm.dto.LlmDtos.ChatCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.ChatMessageDto;
import ai.nova.platform.llm.dto.LlmDtos.MessageView;
import ai.nova.platform.llm.provider.LlmCompletionRequest.LlmChatMessage;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class ContextService {

    private final LlmConversationService conversationService;

    public ContextService(LlmConversationService conversationService) {
        this.conversationService = conversationService;
    }

    public List<LlmChatMessage> buildMessages(ChatCompletionRequest request, AuthenticatedUser user) {
        List<LlmChatMessage> messages = new ArrayList<>();
        if (request.context() != null && !request.context().isBlank()) {
            messages.add(new LlmChatMessage("system", "Context:\n" + request.context()));
        }
        if (request.conversationId() != null) {
            List<MessageView> history =
                    conversationService.history(request.conversationId(), user);
            for (MessageView message : history) {
                messages.add(new LlmChatMessage(message.role().name().toLowerCase(), message.content()));
            }
        }
        if (request.messages() != null) {
            for (ChatMessageDto message : request.messages()) {
                messages.add(new LlmChatMessage(message.role().toLowerCase(), message.content()));
            }
        }
        return messages;
    }
}
