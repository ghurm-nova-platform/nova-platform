package ai.nova.platform.collaboration.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.CollaborationConfigResponse;
import ai.nova.platform.collaboration.dto.CollaborationDtos.CreateSessionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.MessageView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.ParticipantView;
import ai.nova.platform.collaboration.dto.CollaborationDtos.RecordDecisionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SendMessageRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionSummary;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TimelineEventView;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/collaboration")
@Validated
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @GetMapping
    public List<SessionSummary> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.list(projectId, user);
    }

    @GetMapping("/config")
    public CollaborationConfigResponse config(@AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.config(user);
    }

    @GetMapping("/{id}")
    public SessionDetail get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.get(id, user);
    }

    @GetMapping("/{id}/timeline")
    public List<TimelineEventView> timeline(
            @PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.timeline(id, user);
    }

    @GetMapping("/{id}/participants")
    public List<ParticipantView> participants(
            @PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.participants(id, user);
    }

    @GetMapping("/{id}/messages")
    public List<MessageView> messages(
            @PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.messages(id, user);
    }

    @PostMapping({"", "/"})
    public SessionDetail create(
            @Valid @RequestBody CreateSessionRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.create(request, user);
    }

    @PostMapping("/{id}/assign")
    public SessionDetail assign(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AssignTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.assign(id, request, user);
    }

    @PostMapping("/{id}/message")
    public SessionDetail message(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.sendMessage(id, request, user);
    }

    @PostMapping("/{id}/decision")
    public SessionDetail decision(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecordDecisionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.recordDecision(id, request, user);
    }

    @PostMapping("/{id}/pause")
    public SessionDetail pause(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.pause(id, user);
    }

    @PostMapping("/{id}/resume")
    public SessionDetail resume(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.resume(id, user);
    }

    @PostMapping("/{id}/cancel")
    public SessionDetail cancel(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return collaborationService.cancel(id, user);
    }
}
