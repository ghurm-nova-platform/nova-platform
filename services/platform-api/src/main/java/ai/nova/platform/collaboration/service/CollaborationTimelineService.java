package ai.nova.platform.collaboration.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.collaboration.entity.CollaborationParticipantRole;
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventEntity;
import ai.nova.platform.collaboration.entity.CollaborationTimelineEventType;
import ai.nova.platform.collaboration.repository.CollaborationTimelineEventRepository;

@Service
public class CollaborationTimelineService {

    private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {};

    private final CollaborationTimelineEventRepository timelineEventRepository;
    private final ObjectMapper objectMapper;

    public CollaborationTimelineService(
            CollaborationTimelineEventRepository timelineEventRepository, ObjectMapper objectMapper) {
        this.timelineEventRepository = timelineEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CollaborationTimelineEventEntity append(
            UUID sessionId,
            UUID organizationId,
            CollaborationTimelineEventType eventType,
            String summary,
            CollaborationParticipantRole actorRole,
            UUID taskId,
            UUID messageId,
            UUID decisionId,
            Map<String, Object> details) {
        CollaborationTimelineEventEntity event = new CollaborationTimelineEventEntity(
                UUID.randomUUID(),
                sessionId,
                organizationId,
                eventType,
                summary,
                actorRole,
                taskId,
                messageId,
                decisionId,
                serializeDetails(details),
                Instant.now());
        return timelineEventRepository.save(event);
    }

    public Map<String, Object> parseDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(detailsJson, DETAILS_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of("raw", detailsJson);
        }
    }

    private String serializeDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
