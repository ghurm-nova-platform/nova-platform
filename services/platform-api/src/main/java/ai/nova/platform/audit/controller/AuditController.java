package ai.nova.platform.audit.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.dto.AuditDtos.AuditHistoryResponse;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchRequest;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchResponse;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.service.AuditService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public AuditSearchResponse listRecent(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "entityType", required = false) AuditEntityType entityType,
            @RequestParam(value = "action", required = false) AuditAction action,
            @RequestParam(value = "severity", required = false) AuditSeverity severity,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (projectId != null || entityType != null || action != null || severity != null) {
            return auditService.search(
                    new AuditSearchRequest(
                            null,
                            null,
                            projectId,
                            null,
                            entityType,
                            null,
                            action,
                            severity,
                            null,
                            null,
                            null,
                            page,
                            size),
                    user);
        }
        return auditService.listRecent(page, size, user);
    }

    @GetMapping("/{id}")
    public AuditEvent get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return auditService.get(id, user);
    }

    @GetMapping("/history")
    public AuditHistoryResponse history(
            @RequestParam("entityType") AuditEntityType entityType,
            @RequestParam("entityId") UUID entityId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return auditService.history(entityType, entityId, user);
    }

    @GetMapping("/search")
    public AuditSearchResponse search(
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "entityType", required = false) AuditEntityType entityType,
            @RequestParam(value = "entityId", required = false) UUID entityId,
            @RequestParam(value = "action", required = false) AuditAction action,
            @RequestParam(value = "severity", required = false) AuditSeverity severity,
            @RequestParam(value = "result", required = false) AuditResult result,
            @RequestParam(value = "correlationId", required = false) String correlationId,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return auditService.search(
                new AuditSearchRequest(
                        from,
                        to,
                        projectId,
                        userId,
                        entityType,
                        entityId,
                        action,
                        severity,
                        result,
                        correlationId,
                        requestId,
                        page,
                        size),
                user);
    }
}
