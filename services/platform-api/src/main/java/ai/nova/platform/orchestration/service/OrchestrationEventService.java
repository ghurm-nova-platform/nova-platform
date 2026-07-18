package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.nova.platform.orchestration.entity.AgentOrchestrationEvent;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.repository.AgentOrchestrationEventRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;

/**
 * Appends orchestration events using the run-owned eventSequence counter.
 * Must be called inside the same transaction that owns the run entity version
 * (caller holds and will save the run after incrementing eventSequence).
 */
@Service
public class OrchestrationEventService {

    private final AgentOrchestrationEventRepository eventRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final Clock clock;

    public OrchestrationEventService(
            AgentOrchestrationEventRepository eventRepository,
            AgentOrchestrationRunRepository runRepository,
            Clock clock) {
        this.eventRepository = eventRepository;
        this.runRepository = runRepository;
        this.clock = clock;
    }

    public AgentOrchestrationEvent appendEvent(
            AgentOrchestrationRun run,
            UUID taskId,
            OrchestrationEventType type,
            String payloadJson,
            UUID actor) {
        long next = run.getEventSequence() + 1;
        run.setEventSequence(next);
        Instant now = Instant.now(clock);
        AgentOrchestrationEvent event = new AgentOrchestrationEvent(
                UUID.randomUUID(),
                run.getOrganizationId(),
                run.getProjectId(),
                run.getId(),
                taskId,
                type,
                next,
                payloadJson,
                actor,
                now);
        eventRepository.save(event);
        runRepository.save(run);
        return event;
    }
}
