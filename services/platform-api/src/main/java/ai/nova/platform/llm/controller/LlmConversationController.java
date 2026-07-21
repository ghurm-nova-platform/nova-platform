package ai.nova.platform.llm.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.llm.dto.LlmDtos.AppendMessageRequest;
import ai.nova.platform.llm.dto.LlmDtos.ConversationView;
import ai.nova.platform.llm.dto.LlmDtos.CreateConversationRequest;
import ai.nova.platform.llm.dto.LlmDtos.MessageView;
import ai.nova.platform.llm.service.LlmConversationService;
import ai.nova.platform.llm.service.LlmAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/llm/conversations")
public class LlmConversationController {

    private final LlmConversationService conversationService;
    private final LlmAuthorizationService authorizationService;

    public LlmConversationController(
            LlmConversationService conversationService, LlmAuthorizationService authorizationService) {
        this.conversationService = conversationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ConversationView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return conversationService.list(user);
    }

    @PostMapping
    public ConversationView create(
            @Valid @RequestBody CreateConversationRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return conversationService.create(request, user);
    }

    @GetMapping("/{id}")
    public ConversationView get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return conversationService.get(id, user);
    }

    @GetMapping("/{id}/messages")
    public List<MessageView> history(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return conversationService.history(id, user);
    }

    @PostMapping("/{id}/messages")
    public MessageView append(
            @PathVariable UUID id,
            @Valid @RequestBody AppendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return conversationService.append(id, request, user);
    }

    @PostMapping("/{id}/summary")
    public ConversationView summarize(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return conversationService.summarize(id, user);
    }
}
