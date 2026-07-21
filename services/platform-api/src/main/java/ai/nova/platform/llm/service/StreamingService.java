package ai.nova.platform.llm.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.nova.platform.llm.dto.LlmDtos.ChatCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.CompletionResponse;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.annotation.PreDestroy;

@Service
public class StreamingService {

    private final InferenceService inferenceService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public StreamingService(InferenceService inferenceService) {
        this.inferenceService = inferenceService;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public SseEmitter streamChat(ChatCompletionRequest request, AuthenticatedUser user) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                CompletionResponse response = inferenceService.chatCompletion(request, user);
                if (response.providerType() == LlmProviderType.DETERMINISTIC) {
                    for (String word : splitWords(response.content())) {
                        emitter.send(SseEmitter.event().name("chunk").data(word + " ", MediaType.TEXT_PLAIN));
                    }
                } else {
                    emitter.send(SseEmitter.event()
                            .name("chunk")
                            .data(response.content(), MediaType.TEXT_PLAIN));
                }
                emitter.send(SseEmitter.event().name("done").data(response, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(ex.getMessage(), MediaType.TEXT_PLAIN));
                } catch (IOException ignored) {
                    // ignore
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private static List<String> splitWords(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return List.of(content.trim().split("\\s+"));
    }
}
