package ai.nova.platform.audit.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditCorrelationEntity;
import ai.nova.platform.audit.entity.AuditEntityEntity;
import ai.nova.platform.audit.entity.AuditEventEntity;
import ai.nova.platform.audit.entity.AuditIndexEntity;
import ai.nova.platform.audit.entity.AuditSessionEntity;
import ai.nova.platform.audit.repository.AuditCorrelationRepository;
import ai.nova.platform.audit.repository.AuditEntityRepository;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.audit.repository.AuditIndexRepository;
import ai.nova.platform.audit.repository.AuditSessionRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class AuditStorageService {

    private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {};

    private final AuditProperties properties;
    private final AuditEventRepository eventRepository;
    private final AuditEntityRepository entityRepository;
    private final AuditSessionRepository sessionRepository;
    private final AuditCorrelationRepository correlationRepository;
    private final AuditIndexRepository indexRepository;
    private final AuditFingerprintService fingerprintService;
    private final ObjectMapper objectMapper;

    public AuditStorageService(
            AuditProperties properties,
            AuditEventRepository eventRepository,
            AuditEntityRepository entityRepository,
            AuditSessionRepository sessionRepository,
            AuditCorrelationRepository correlationRepository,
            AuditIndexRepository indexRepository,
            AuditFingerprintService fingerprintService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.eventRepository = eventRepository;
        this.entityRepository = entityRepository;
        this.sessionRepository = sessionRepository;
        this.correlationRepository = correlationRepository;
        this.indexRepository = indexRepository;
        this.fingerprintService = fingerprintService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditEvent append(RecordAuditEventRequest request) {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AUDIT_DISABLED", "Audit center is disabled");
        }
        if (properties.isImmutable()) {
            // append-only contract enforced by exposing only append
        }

        String fingerprint = fingerprintService.fingerprint(request);
        Optional<AuditEventEntity> existing =
                eventRepository.findByOrganizationIdAndEventFingerprint(request.organizationId(), fingerprint);
        if (existing.isPresent()) {
            return toDto(existing.get(), resolveEntityLabel(existing.get()));
        }

        Instant now = Instant.now();
        ensureSession(request, now);
        upsertEntityReference(request, now);

        UUID eventId = UUID.randomUUID();
        AuditEventEntity entity = new AuditEventEntity(
                eventId,
                request.organizationId(),
                request.projectId(),
                request.userId(),
                truncate(request.username(), 200),
                request.sessionId(),
                request.entityType(),
                request.entityId(),
                request.action(),
                request.result(),
                request.severity(),
                request.source(),
                truncate(request.correlationId(), 64),
                truncate(request.requestId(), 64),
                truncate(request.ipAddress(), 64),
                truncate(request.userAgent(), 500),
                toDetailsJson(request.details()),
                fingerprint,
                now);

        try {
            eventRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            return eventRepository
                    .findByOrganizationIdAndEventFingerprint(request.organizationId(), fingerprint)
                    .map(saved -> toDto(saved, resolveEntityLabel(saved)))
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.CONFLICT, "AUDIT_DUPLICATE_EVENT", "Duplicate audit event fingerprint"));
        }

        appendCorrelation(request, eventId, now);
        appendIndexes(request, eventId, now);
        return toDto(entity, request.entityLabel());
    }

    @Transactional
    public AuditSessionEntity startSession(
            UUID sessionId, UUID userId, UUID organizationId, String ipAddress, String userAgent, Instant now) {
        AuditSessionEntity session = new AuditSessionEntity(
                UUID.randomUUID(),
                sessionId,
                userId,
                organizationId,
                now,
                null,
                truncate(ipAddress, 64),
                truncate(userAgent, 500));
        return sessionRepository.save(session);
    }

    @Transactional
    public void endSession(UUID sessionId, Instant now) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setEndedAt(now);
            sessionRepository.save(session);
        });
    }

    @Transactional
    public void rejectMutation() {
        if (properties.isImmutable()) {
            throw new ApiException(HttpStatus.CONFLICT, "AUDIT_IMMUTABLE", "Audit records are immutable");
        }
    }

    public AuditEvent toDto(AuditEventEntity entity) {
        return toDto(entity, resolveEntityLabel(entity));
    }

    public AuditEvent toDto(AuditEventEntity entity, String entityLabel) {
        return new AuditEvent(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getUserId(),
                entity.getUsername(),
                entity.getSessionId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entityLabel,
                entity.getAction(),
                entity.getResult(),
                entity.getSeverity(),
                entity.getSource(),
                entity.getCorrelationId(),
                entity.getRequestId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                parseDetails(entity.getDetailsJson()),
                entity.getCreatedAt());
    }

    private void ensureSession(RecordAuditEventRequest request, Instant now) {
        if (request.sessionId() == null) {
            return;
        }
        sessionRepository.findBySessionId(request.sessionId()).orElseGet(() -> sessionRepository.save(
                new AuditSessionEntity(
                        UUID.randomUUID(),
                        request.sessionId(),
                        request.userId(),
                        request.organizationId(),
                        now,
                        null,
                        truncate(request.ipAddress(), 64),
                        truncate(request.userAgent(), 500))));
    }

    private void upsertEntityReference(RecordAuditEventRequest request, Instant now) {
        if (request.entityId() == null) {
            return;
        }
        entityRepository
                .findByOrganizationIdAndEntityTypeAndEntityId(
                        request.organizationId(), request.entityType(), request.entityId())
                .ifPresentOrElse(
                        existing -> {
                            if (request.entityLabel() != null
                                    && !request.entityLabel().equals(existing.getDisplayLabel())) {
                                existing.setDisplayLabel(truncate(request.entityLabel(), 500));
                                existing.setUpdatedAt(now);
                                entityRepository.save(existing);
                            }
                        },
                        () -> entityRepository.save(new AuditEntityEntity(
                                UUID.randomUUID(),
                                request.organizationId(),
                                request.entityType(),
                                request.entityId(),
                                truncate(request.entityLabel(), 500),
                                now,
                                now)));
    }

    private void appendCorrelation(RecordAuditEventRequest request, UUID eventId, Instant now) {
        if (request.correlationId() == null && request.requestId() == null && request.sessionId() == null) {
            return;
        }
        correlationRepository.save(new AuditCorrelationEntity(
                UUID.randomUUID(),
                request.organizationId(),
                request.correlationId(),
                request.requestId(),
                request.sessionId(),
                eventId,
                0,
                now));
    }

    private void appendIndexes(RecordAuditEventRequest request, UUID eventId, Instant now) {
        List<AuditIndexEntity> indexes = new ArrayList<>();
        if (request.entityId() != null) {
            indexes.add(index(
                    request.organizationId(),
                    eventId,
                    "entity_id",
                    request.entityId().toString(),
                    now));
        }
        if (request.correlationId() != null) {
            indexes.add(index(
                    request.organizationId(), eventId, "correlation_id", request.correlationId(), now));
        }
        if (request.requestId() != null) {
            indexes.add(index(request.organizationId(), eventId, "request_id", request.requestId(), now));
        }
        if (!indexes.isEmpty()) {
            indexRepository.saveAll(indexes);
        }
    }

    private AuditIndexEntity index(UUID organizationId, UUID eventId, String key, String value, Instant now) {
        return new AuditIndexEntity(UUID.randomUUID(), organizationId, eventId, key, truncate(value, 500), now);
    }

    private String resolveEntityLabel(AuditEventEntity entity) {
        if (entity.getEntityId() == null) {
            return null;
        }
        return entityRepository
                .findByOrganizationIdAndEntityTypeAndEntityId(
                        entity.getOrganizationId(), entity.getEntityType(), entity.getEntityId())
                .map(AuditEntityEntity::getDisplayLabel)
                .orElse(null);
    }

    private Map<String, Object> parseDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(detailsJson, DETAILS_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of("raw", detailsJson);
        }
    }

    private String toDetailsJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"details serialization failed\"}";
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
