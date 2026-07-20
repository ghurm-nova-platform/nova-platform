package ai.nova.platform.policy.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ai.nova.platform.policy.entity.EvaluationMode;
import ai.nova.platform.policy.entity.PolicyType;
import ai.nova.platform.web.error.ApiException;

@Service
public class PolicyHashService {

    private final ObjectMapper objectMapper;

    public PolicyHashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String fingerprint(
            UUID organizationId,
            UUID projectId,
            String policyName,
            PolicyType policyType,
            EvaluationMode mode,
            int priority,
            Map<String, Object> configuration) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("organizationId", organizationId.toString());
        root.put("projectId", projectId.toString());
        root.put("policyName", policyName.trim());
        root.put("policyType", policyType.name());
        root.put("evaluationMode", mode.name());
        root.put("priority", priority);
        root.put("configuration", configuration == null ? Map.of() : new TreeMap<>(configuration));
        return sha256(toJson(root));
    }

    public String evaluationHash(
            UUID policyId,
            UUID policyVersionId,
            UUID releaseId,
            String semanticVersion,
            String manifestHash,
            String contentFingerprint,
            String configJson) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("policyId", policyId.toString());
        root.put("policyVersionId", policyVersionId.toString());
        root.put("releaseId", releaseId.toString());
        root.put("semanticVersion", semanticVersion == null ? "" : semanticVersion);
        root.put("manifestHash", manifestHash == null ? "" : manifestHash);
        root.put("contentFingerprint", contentFingerprint == null ? "" : contentFingerprint);
        root.put("configJson", configJson == null ? "" : configJson);
        return sha256(toJson(root));
    }

    public String toConfigJson(Map<String, Object> configuration) {
        try {
            Map<String, Object> sorted =
                    configuration == null || configuration.isEmpty() ? Map.of() : new TreeMap<>(configuration);
            return objectMapper.writeValueAsString(sorted);
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "POLICY_INVALID_CONFIGURATION", "Invalid policy configuration");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseConfig(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "POLICY_ENGINE_ERROR", "Failed to parse policy configuration");
        }
    }

    private String toJson(Map<String, Object> root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "POLICY_ENGINE_ERROR", "Failed to serialize policy hash");
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
