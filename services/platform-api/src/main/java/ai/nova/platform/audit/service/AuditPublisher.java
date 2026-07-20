package ai.nova.platform.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;

/**
 * Internal append-only publisher. Uses REQUIRES_NEW so audit rows survive business rollbacks.
 * Failures are swallowed so audit never breaks business flows (e.g. login).
 */
@Service
public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

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
        try {
            return storageService.append(request);
        } catch (RuntimeException ex) {
            log.warn(
                    "Audit publish failed (org={}, action={}, entityType={}): {}",
                    request.organizationId(),
                    request.action(),
                    request.entityType(),
                    ex.toString());
            return null;
        }
    }
}
