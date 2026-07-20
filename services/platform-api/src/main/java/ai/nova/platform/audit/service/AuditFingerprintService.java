package ai.nova.platform.audit.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;

@Service
public class AuditFingerprintService {

    private final ObjectMapper objectMapper;

    public AuditFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String fingerprint(RecordAuditEventRequest request) {
        String payload = String.join(
                "|",
                safe(request.organizationId()),
                safe(request.projectId()),
                safe(request.userId()),
                safe(request.username()),
                safe(request.sessionId()),
                safe(request.entityType()),
                safe(request.entityId()),
                safe(request.action()),
                safe(request.result()),
                safe(request.severity()),
                safe(request.source()),
                safe(request.correlationId()),
                safe(request.requestId()),
                detailsJson(request.details()));
        return sha256(payload);
    }

    public String fingerprint(
            UUID organizationId,
            UUID projectId,
            UUID userId,
            AuditEntityType entityType,
            UUID entityId,
            AuditAction action,
            AuditResult result,
            AuditSeverity severity,
            AuditSource source,
            String correlationId,
            String requestId,
            Map<String, Object> details) {
        return fingerprint(new RecordAuditEventRequest(
                organizationId,
                projectId,
                userId,
                null,
                null,
                entityType,
                entityId,
                null,
                action,
                result,
                severity,
                source,
                correlationId,
                requestId,
                null,
                null,
                details));
    }

    private String detailsJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return details.toString();
        }
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
