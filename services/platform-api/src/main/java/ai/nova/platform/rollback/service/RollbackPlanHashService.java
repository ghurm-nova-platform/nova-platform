package ai.nova.platform.rollback.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ai.nova.platform.rollback.entity.RollbackRiskLevel;
import ai.nova.platform.rollback.entity.RollbackStrategy;
import ai.nova.platform.web.error.ApiException;

/**
 * Builds immutable rollback plan JSON and SHA-256 hash over canonical JSON.
 */
@Service
public class RollbackPlanHashService {

    private final ObjectMapper objectMapper;

    public RollbackPlanHashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public record PlanHashResult(String planJson, String planHash) {
    }

    public PlanHashResult build(
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID deploymentId,
            UUID targetReleaseId,
            String environmentCode,
            RollbackStrategy strategy,
            String reason,
            RollbackRiskLevel riskLevel,
            String currentVersion,
            String targetVersion) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("organizationId", organizationId.toString());
        root.put("projectId", projectId.toString());
        root.put("releaseId", releaseId.toString());
        root.put("deploymentId", deploymentId.toString());
        root.put("targetReleaseId", targetReleaseId.toString());
        root.put("environment", environmentCode == null ? "" : environmentCode.toUpperCase());
        root.put("strategy", strategy.name());
        root.put("reason", reason == null ? "" : reason.trim());
        root.put("riskLevel", riskLevel.name());
        root.put("currentVersion", currentVersion);
        root.put("targetVersion", targetVersion);
        try {
            String json = objectMapper.writeValueAsString(root);
            return new PlanHashResult(json, sha256(json));
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "ROLLBACK_VALIDATION_FAILED", "Failed to serialize rollback plan");
        }
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
