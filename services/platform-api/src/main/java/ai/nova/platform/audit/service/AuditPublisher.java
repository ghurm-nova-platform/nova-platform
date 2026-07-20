package ai.nova.platform.audit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;

/**
 * Internal append-only publisher. Uses REQUIRES_NEW so audit rows survive business rollbacks.
 */
@Service
public class AuditPublisher {

    private final AuditProperties properties;
    private final AuditStorageService storageService;

    public AuditPublisher(AuditProperties properties, AuditStorageService storageService) {
        this.properties = properties;
        this.storageService = storageService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent record(RecordAuditEventRequest request) {
        if (!properties.isEnabled()) {
            return null;
        }
        return storageService.append(request);
    }
}
