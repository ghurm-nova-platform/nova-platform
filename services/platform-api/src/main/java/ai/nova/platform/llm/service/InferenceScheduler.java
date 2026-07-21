package ai.nova.platform.llm.service;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import ai.nova.platform.llm.configuration.LlmProperties;
import ai.nova.platform.llm.dto.LlmDtos.ChatCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.CompletionResponse;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class InferenceScheduler {

    private final LlmProperties properties;
    private final InferenceService inferenceService;
    private BlockingQueue<QueuedJob> queue;
    private ExecutorService workers;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public InferenceScheduler(LlmProperties properties, InferenceService inferenceService) {
        this.properties = properties;
        this.inferenceService = inferenceService;
    }

    @PostConstruct
    void start() {
        if (!properties.getScheduler().isEnabled()) {
            return;
        }
        int capacity = Math.max(1, properties.getScheduler().getQueueCapacity());
        queue = new LinkedBlockingQueue<>(capacity);
        int workersCount = Math.max(1, properties.getScheduler().getWorkerCount());
        workers = Executors.newFixedThreadPool(workersCount);
        running.set(true);
        for (int i = 0; i < workersCount; i++) {
            workers.submit(this::drain);
        }
    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (workers != null) {
            workers.shutdownNow();
        }
    }

    public String submit(ChatCompletionRequest request, AuthenticatedUser user, int priority) {
        if (queue == null) {
            inferenceService.chatCompletion(request, user);
            return UUID.randomUUID().toString();
        }
        String jobId = UUID.randomUUID().toString();
        QueuedJob job = new QueuedJob(jobId, priority, request, user);
        if (!queue.offer(job)) {
            throw new IllegalStateException("Inference queue is full");
        }
        return jobId;
    }

    public CompletionResponse submitAndWait(ChatCompletionRequest request, AuthenticatedUser user) {
        return inferenceService.chatCompletion(request, user);
    }

    private void drain() {
        while (running.get()) {
            try {
                QueuedJob job = queue.take();
                inferenceService.chatCompletion(job.request(), job.user());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ignored) {
                // keep worker alive
            }
        }
    }

    private record QueuedJob(String id, int priority, ChatCompletionRequest request, AuthenticatedUser user)
            implements Comparable<QueuedJob> {
        @Override
        public int compareTo(QueuedJob other) {
            return Integer.compare(other.priority, this.priority);
        }
    }
}
