package ai.nova.platform.audit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.dto.AuditDtos.AuditCorrelationView;
import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.AuditHistoryResponse;
import ai.nova.platform.audit.entity.AuditCorrelationEntity;
import ai.nova.platform.audit.entity.AuditEntityEntity;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditEventEntity;
import ai.nova.platform.audit.repository.AuditCorrelationRepository;
import ai.nova.platform.audit.repository.AuditEntityRepository;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.audit.security.AuditAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AuditCorrelationService {

    private final AuditProperties properties;
    private final AuditAuthorizationService authorizationService;
    private final AuditEventRepository eventRepository;
    private final AuditCorrelationRepository correlationRepository;
    private final AuditEntityRepository entityRepository;
    private final AuditStorageService storageService;

    public AuditCorrelationService(
            AuditProperties properties,
            AuditAuthorizationService authorizationService,
            AuditEventRepository eventRepository,
            AuditCorrelationRepository correlationRepository,
            AuditEntityRepository entityRepository,
            AuditStorageService storageService) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.eventRepository = eventRepository;
        this.correlationRepository = correlationRepository;
        this.entityRepository = entityRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public AuditHistoryResponse entityHistory(AuditEntityType entityType, UUID entityId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        if (entityType == null || entityId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "AUDIT_INVALID_QUERY", "entityType and entityId are required");
        }
        String label = entityRepository
                .findByOrganizationIdAndEntityTypeAndEntityId(user.getOrganizationId(), entityType, entityId)
                .map(AuditEntityEntity::getDisplayLabel)
                .orElse(null);
        List<AuditEvent> events = eventRepository
                .findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        user.getOrganizationId(), entityType, entityId)
                .stream()
                .map(entity -> storageService.toDto(entity, label))
                .toList();
        return new AuditHistoryResponse(entityType, entityId, label, events);
    }

    @Transactional(readOnly = true)
    public AuditCorrelationView byCorrelationId(String correlationId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        if (correlationId == null || correlationId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUDIT_INVALID_QUERY", "correlationId is required");
        }
        List<AuditCorrelationEntity> links = correlationRepository.findByOrganizationIdAndCorrelationIdOrderByCreatedAtAscChainSequenceAsc(
                user.getOrganizationId(), correlationId.trim());
        List<AuditEvent> events = loadEvents(user.getOrganizationId(), links);
        return new AuditCorrelationView(correlationId.trim(), null, null, events);
    }

    @Transactional(readOnly = true)
    public AuditCorrelationView byRequestId(String requestId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        if (requestId == null || requestId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUDIT_INVALID_QUERY", "requestId is required");
        }
        List<AuditCorrelationEntity> links = correlationRepository.findByOrganizationIdAndRequestIdOrderByCreatedAtAscChainSequenceAsc(
                user.getOrganizationId(), requestId.trim());
        List<AuditEvent> events = loadEvents(user.getOrganizationId(), links);
        return new AuditCorrelationView(null, requestId.trim(), null, events);
    }

    private List<AuditEvent> loadEvents(UUID organizationId, List<AuditCorrelationEntity> links) {
        return links.stream()
                .map(link -> eventRepository.findByIdAndOrganizationId(link.getAuditEventId(), organizationId))
                .flatMap(java.util.Optional::stream)
                .map(storageService::toDto)
                .toList();
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AUDIT_DISABLED", "Audit center is disabled");
        }
    }
}
