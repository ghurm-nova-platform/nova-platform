package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.orchestration.entity.AgentOrchestrationEvent;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.repository.AgentOrchestrationEventRepository;
import ai.nova.platform.web.error.ApiException;

/**
 * Appends orchestration events using a dedicated counter table.
 * Sequence reservation is atomic via {@code SELECT … FOR UPDATE} on the counter row and does not
 * touch {@code AgentOrchestrationRun.version}, so concurrent task claim/completion transactions
 * do not roll back solely due to audit sequencing.
 */
@Service
public class OrchestrationEventService {

    private final AgentOrchestrationEventRepository eventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public OrchestrationEventService(
            AgentOrchestrationEventRepository eventRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock) {
        this.eventRepository = eventRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    /**
     * Ensures a counter row exists for the run. Safe to call after run insert.
     */
    @Transactional
    public void ensureCounter(UUID runId) {
        Integer exists = jdbcTemplate.query(
                "SELECT 1 FROM agent_orchestration_event_counters WHERE run_id = ?",
                rs -> rs.next() ? 1 : null,
                runId);
        if (exists == null) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO agent_orchestration_event_counters (run_id, next_sequence) VALUES (?, 0)",
                        runId);
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                // Concurrent ensure — counter row already created.
            }
        }
    }

    public AgentOrchestrationEvent appendEvent(
            AgentOrchestrationRun run,
            UUID taskId,
            OrchestrationEventType type,
            String payloadJson,
            UUID actor) {
        return appendEvent(run.getId(), run.getOrganizationId(), run.getProjectId(), taskId, type, payloadJson, actor);
    }

    public AgentOrchestrationEvent appendEvent(
            UUID runId,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            OrchestrationEventType type,
            String payloadJson,
            UUID actor) {
        long sequence = reserveNextSequence(runId);
        Instant now = Instant.now(clock);
        AgentOrchestrationEvent event = new AgentOrchestrationEvent(
                UUID.randomUUID(),
                organizationId,
                projectId,
                runId,
                taskId,
                type,
                sequence,
                payloadJson,
                actor,
                now);
        try {
            eventRepository.saveAndFlush(event);
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ORCHESTRATION_EVENT_APPEND_FAILED",
                    "Failed to append orchestration event " + type.name());
        }
        // Best-effort mirror for operators; never fail business TX if mirror lags.
        try {
            jdbcTemplate.update(
                    "UPDATE agent_orchestration_runs SET event_sequence = ? WHERE id = ?",
                    sequence,
                    runId);
        } catch (DataAccessException ignored) {
            // Counter + event row remain authoritative.
        }
        return event;
    }

    /**
     * Reserves the next monotonically increasing sequence for the run.
     * Locks only the counter row (short) — not the run optimistic-lock version.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    protected long reserveNextSequence(UUID runId) {
        ensureCounter(runId);
        Long current = jdbcTemplate.query(
                "SELECT next_sequence FROM agent_orchestration_event_counters WHERE run_id = ? FOR UPDATE",
                rs -> rs.next() ? rs.getLong(1) : null,
                runId);
        if (current == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ORCHESTRATION_EVENT_SEQUENCE_FAILED",
                    "Failed to reserve orchestration event sequence");
        }
        long next = current + 1;
        int updated = jdbcTemplate.update(
                "UPDATE agent_orchestration_event_counters SET next_sequence = ? WHERE run_id = ?",
                next,
                runId);
        if (updated != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ORCHESTRATION_EVENT_SEQUENCE_FAILED",
                    "Failed to persist orchestration event sequence");
        }
        return next;
    }
}
