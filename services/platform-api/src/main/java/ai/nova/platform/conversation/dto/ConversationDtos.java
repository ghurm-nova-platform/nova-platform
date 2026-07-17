package ai.nova.platform.conversation.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ai.nova.platform.conversation.entity.ConversationMessageRole;
import ai.nova.platform.conversation.entity.ConversationStatus;

public final class ConversationDtos {

    private ConversationDtos() {
    }

    public record ConversationCreateRequest(@NotNull UUID agentId, String title) {
    }

    public record ConversationUpdateRequest(@NotBlank String title, @NotNull Integer version) {
    }

    public record ConversationResponse(
            UUID id,
            UUID projectId,
            UUID agentId,
            String title,
            ConversationStatus status,
            int messageCount,
            Instant lastMessageAt,
            Instant createdAt,
            Instant updatedAt,
            Integer version) {
    }

    public record ConversationMessageCreateRequest(@NotBlank String content) {
    }

    public record ConversationMessageResponse(
            UUID id,
            ConversationMessageRole role,
            String content,
            int sequenceNumber,
            UUID executionId,
            Instant createdAt) {
    }
}
