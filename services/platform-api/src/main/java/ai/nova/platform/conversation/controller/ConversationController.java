package ai.nova.platform.conversation.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.conversation.dto.ConversationDtos.ConversationCreateRequest;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationMessageCreateRequest;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationMessageResponse;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationResponse;
import ai.nova.platform.conversation.dto.ConversationDtos.ConversationUpdateRequest;
import ai.nova.platform.conversation.entity.ConversationStatus;
import ai.nova.platform.conversation.service.ConversationService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody ConversationCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.create(projectId, request, user);
    }

    @GetMapping
    public Page<ConversationResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) ConversationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = {"lastMessageAt", "createdAt"}, direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return conversationService.list(projectId, user, agentId, status, search, pageable);
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.get(projectId, conversationId, user);
    }

    @PutMapping("/{conversationId}")
    public ConversationResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.update(projectId, conversationId, request, user);
    }

    @DeleteMapping("/{conversationId}")
    public ConversationResponse archive(
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.archive(projectId, conversationId, user);
    }

    @PostMapping("/{conversationId}/restore")
    public ConversationResponse restore(
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.restore(projectId, conversationId, user);
    }

    @GetMapping("/{conversationId}/messages")
    public Page<ConversationMessageResponse> listMessages(
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @PageableDefault(size = 20, sort = "sequenceNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return conversationService.listMessages(projectId, conversationId, user, pageable);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationMessageResponse addUserMessage(
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationMessageCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.addUserMessage(projectId, conversationId, request, user);
    }
}
