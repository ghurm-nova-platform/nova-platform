package ai.nova.platform.llm.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.llm.dto.LlmDtos.ChatCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.CompletionResponse;
import ai.nova.platform.security.AuthenticatedUser;

/** Thin facade over {@link InferenceScheduler}. */
@Service
public class SchedulerService {

    private final InferenceScheduler inferenceScheduler;

    public SchedulerService(InferenceScheduler inferenceScheduler) {
        this.inferenceScheduler = inferenceScheduler;
    }

    public String submit(ChatCompletionRequest request, AuthenticatedUser user) {
        return inferenceScheduler.submit(request, user, 0);
    }

    public CompletionResponse execute(ChatCompletionRequest request, AuthenticatedUser user) {
        return inferenceScheduler.submitAndWait(request, user);
    }
}
