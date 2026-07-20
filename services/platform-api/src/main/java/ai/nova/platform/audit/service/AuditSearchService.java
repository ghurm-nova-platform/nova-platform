package ai.nova.platform.audit.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchRequest;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchResponse;
import ai.nova.platform.audit.entity.AuditEventEntity;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.audit.security.AuditAuthorizationService;
import ai.nova.platform.audit.config.AuditProperties;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

import jakarta.persistence.criteria.Predicate;

@Service
public class AuditSearchService {

    private final AuditProperties properties;
    private final AuditAuthorizationService authorizationService;
    private final AuditEventRepository eventRepository;
    private final AuditStorageService storageService;

    public AuditSearchService(
            AuditProperties properties,
            AuditAuthorizationService authorizationService,
            AuditEventRepository eventRepository,
            AuditStorageService storageService) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.eventRepository = eventRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public AuditSearchResponse search(AuditSearchRequest request, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        validateQuery(request);

        try {
            Specification<AuditEventEntity> spec = buildSpecification(user.getOrganizationId(), request);
            int page = Math.max(request.page(), 0);
            int size = Math.min(Math.max(request.size(), 1), 200);
            Page<AuditEventEntity> result = eventRepository.findAll(
                    spec,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
            List<AuditEvent> events = result.getContent().stream().map(storageService::toDto).toList();
            return new AuditSearchResponse(events, result.getTotalElements(), page, size);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIT_SEARCH_FAILED", "Audit search failed");
        }
    }

    @Transactional(readOnly = true)
    public AuditSearchResponse listRecent(int page, int size, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Page<AuditEventEntity> result = eventRepository.findByOrganizationIdOrderByCreatedAtDesc(
                user.getOrganizationId(), PageRequest.of(safePage, safeSize));
        List<AuditEvent> events = result.getContent().stream().map(storageService::toDto).toList();
        return new AuditSearchResponse(events, result.getTotalElements(), safePage, safeSize);
    }

    private Specification<AuditEventEntity> buildSpecification(
            java.util.UUID organizationId, AuditSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            if (request.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.from()));
            }
            if (request.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.to()));
            }
            if (request.projectId() != null) {
                predicates.add(cb.equal(root.get("projectId"), request.projectId()));
            }
            if (request.userId() != null) {
                predicates.add(cb.equal(root.get("userId"), request.userId()));
            }
            if (request.entityType() != null) {
                predicates.add(cb.equal(root.get("entityType"), request.entityType()));
            }
            if (request.entityId() != null) {
                predicates.add(cb.equal(root.get("entityId"), request.entityId()));
            }
            if (request.action() != null) {
                predicates.add(cb.equal(root.get("action"), request.action()));
            }
            if (request.severity() != null) {
                predicates.add(cb.equal(root.get("severity"), request.severity()));
            }
            if (request.result() != null) {
                predicates.add(cb.equal(root.get("result"), request.result()));
            }
            if (request.correlationId() != null && !request.correlationId().isBlank()) {
                predicates.add(cb.equal(root.get("correlationId"), request.correlationId().trim()));
            }
            if (request.requestId() != null && !request.requestId().isBlank()) {
                predicates.add(cb.equal(root.get("requestId"), request.requestId().trim()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateQuery(AuditSearchRequest request) {
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "AUDIT_INVALID_QUERY", "from must be before or equal to to");
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AUDIT_DISABLED", "Audit center is disabled");
        }
    }
}
