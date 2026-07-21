package ai.nova.platform.llm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.nova.platform.llm.dto.LlmDtos.BatchCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.ChatCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.CompletionResponse;
import ai.nova.platform.llm.dto.LlmDtos.TextCompletionRequest;
import ai.nova.platform.llm.service.InferenceService;
import ai.nova.platform.llm.service.LlmAuthorizationService;
import ai.nova.platform.llm.service.StreamingService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/llm")
public class LlmInferenceController {

    private final InferenceService inferenceService;
    private final StreamingService streamingService;
    private final LlmAuthorizationService authorizationService;

    public LlmInferenceController(
            InferenceService inferenceService,
            StreamingService streamingService,
            LlmAuthorizationService authorizationService) {
        this.inferenceService = inferenceService;
        this.streamingService = streamingService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/chat")
    public CompletionResponse chat(
            @Valid @RequestBody ChatCompletionRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return inferenceService.chatCompletion(request, user);
    }

    @PostMapping("/completions")
    public CompletionResponse completions(
            @Valid @RequestBody TextCompletionRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return inferenceService.textCompletion(request, user);
    }

    @PostMapping("/batch")
    public List<CompletionResponse> batch(
            @Valid @RequestBody BatchCompletionRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return inferenceService.batch(request, user);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @Valid @RequestBody ChatCompletionRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return streamingService.streamChat(request, user);
    }

    @PostMapping("/cancel/{token}")
    public Map<String, Object> cancel(
            @PathVariable("token") String token, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.requireInfer(user);
        return Map.of("cancelled", inferenceService.cancel(token));
    }
}
