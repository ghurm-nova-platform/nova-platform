package ai.nova.platform.llm.provider;

import java.util.List;

public record LlmCompletionRequest(
        String modelCode,
        List<LlmChatMessage> messages,
        Integer maxTokens,
        Double temperature,
        boolean stream) {

    public record LlmChatMessage(String role, String content) {
    }
}
