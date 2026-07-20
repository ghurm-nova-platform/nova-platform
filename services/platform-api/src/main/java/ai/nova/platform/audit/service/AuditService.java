package ai.nova.platform.audit.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.AuditHistoryResponse;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchRequest;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchResponse;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.audit.security.AuditAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AuditService {

    private final AuditProperties properties;
    private final AuditPublisher publisher;
    private final AuditSearchService searchService;
    private final AuditCorrelationService correlationService;
    private final AuditEventRepository eventRepository;
    private final AuditStorageService storageService;
    private final AuditAuthorizationService authorizationService;

    public AuditService(
            AuditProperties properties,
            AuditPublisher publisher,
            AuditSearchService searchService,
            AuditCorrelationService correlationService,
            AuditEventRepository eventRepository,
            AuditStorageService storageService,
            AuditAuthorizationService authorizationService) {
        this.properties = properties;
        this.publisher = publisher;
        this.searchService = searchService;
        this.correlationService = correlationService;
        this.eventRepository = eventRepository;
        this.storageService = storageService;
        this.authorizationService = authorizationService;
    }

    public AuditEvent record(RecordAuditEventRequest request) {
        return publisher.record(request);
    }

    @Transactional(readOnly = true)
    public AuditSearchResponse listRecent(int page, int size, AuthenticatedUser user) {
        return searchService.listRecent(page, size, user);
    }

    @Transactional(readOnly = true)
    public AuditEvent get(UUID id, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        return eventRepository
                .findByIdAndOrganizationId(id, user.getOrganizationId())
                .map(storageService::toDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AUDIT_NOT_FOUND", "Audit event not found"));
    }

    @Transactional(readOnly = true)
    public AuditHistoryResponse history(AuditEntityType entityType, UUID entityId, AuthenticatedUser user) {
        return correlationService.entityHistory(entityType, entityId, user);
    }

    @Transactional(readOnly = true)
    public AuditSearchResponse search(AuditSearchRequest request, AuthenticatedUser user) {
        return searchService.search(request, user);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AUDIT_DISABLED", "Audit center is disabled");
        }
    }
}
