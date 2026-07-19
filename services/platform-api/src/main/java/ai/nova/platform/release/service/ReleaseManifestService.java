package ai.nova.platform.release.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ai.nova.platform.release.entity.ReleaseArtifactEntity;
import ai.nova.platform.release.entity.ReleaseContentEntity;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.web.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Builds an immutable release manifest and SHA-256 hash over canonical JSON.
 */
@Service
public class ReleaseManifestService {

    private final ObjectMapper objectMapper;

    public ReleaseManifestService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public record ManifestResult(String manifestJson, String manifestHash) {
    }

    public ManifestResult build(
            ReleaseOperationEntity release,
            List<ReleaseContentEntity> contents,
            List<ReleaseArtifactEntity> artifacts) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("releaseId", release.getId().toString());
        root.put("releaseNumber", release.getReleaseNumber());
        root.put("semanticVersion", release.getSemanticVersion());
        root.put("releaseName", release.getReleaseName());
        root.put("description", release.getDescription() == null ? "" : release.getDescription());
        root.put("organizationId", release.getOrganizationId().toString());
        root.put("projectId", release.getProjectId().toString());
        root.put("createdBy", release.getCreatedBy().toString());
        root.put("createdAt", release.getCreatedAt().toString());
        root.put("contentFingerprint", release.getContentFingerprint());
        root.put("versionStrategy", release.getVersionStrategy().name());
        root.put("bumpType", release.getBumpType() == null ? null : release.getBumpType().name());

        List<String> mergeIds = refs(contents, ReleaseContentType.MERGE_OPERATION);
        List<String> approvalIds = refs(contents, ReleaseContentType.APPROVAL_DECISION);
        List<String> pullRequestIds = refs(contents, ReleaseContentType.PULL_REQUEST);
        List<String> patchIds = refs(contents, ReleaseContentType.PATCH);
        List<String> commits = contents.stream()
                .filter(c -> c.getContentType() == ReleaseContentType.COMMIT)
                .map(ReleaseContentEntity::getCommitSha)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        root.put("mergeOperationIds", mergeIds);
        root.put("approvalDecisionIds", approvalIds);
        root.put("pullRequestIds", pullRequestIds);
        root.put("patchIds", patchIds);
        root.put("commitShas", commits);

        List<Map<String, Object>> artifactMaps = new ArrayList<>();
        List<ReleaseArtifactEntity> sortedArtifacts = artifacts.stream()
                .sorted(Comparator.comparing(ReleaseArtifactEntity::getArtifactUri)
                        .thenComparing(ReleaseArtifactEntity::getArtifactType))
                .toList();
        for (ReleaseArtifactEntity artifact : sortedArtifacts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("artifactType", artifact.getArtifactType());
            item.put("artifactUri", artifact.getArtifactUri());
            item.put("artifactHash", artifact.getArtifactHash() == null ? "" : artifact.getArtifactHash());
            item.put("label", artifact.getLabel() == null ? "" : artifact.getLabel());
            artifactMaps.add(item);
        }
        root.put("artifacts", artifactMaps);

        try {
            String json = objectMapper.writeValueAsString(root);
            return new ManifestResult(json, sha256(json));
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RELEASE_MANIFEST_CHANGED", "Failed to serialize release manifest");
        }
    }

    public String contentFingerprint(
            List<UUID> mergeOperationIds,
            List<UUID> approvalDecisionIds,
            List<UUID> pullRequestIds,
            List<UUID> patchIds,
            List<String> commitShas,
            List<ArtifactFingerprint> artifacts) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("mergeOperationIds", sortedUuids(mergeOperationIds));
        root.put("approvalDecisionIds", sortedUuids(approvalDecisionIds));
        root.put("pullRequestIds", sortedUuids(pullRequestIds));
        root.put("patchIds", sortedUuids(patchIds));
        root.put(
                "commitShas",
                commitShas == null
                        ? List.of()
                        : commitShas.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).sorted().toList());
        List<Map<String, String>> arts = new ArrayList<>();
        if (artifacts != null) {
            for (ArtifactFingerprint a : artifacts.stream()
                    .sorted(Comparator.comparing(ArtifactFingerprint::uri).thenComparing(ArtifactFingerprint::type))
                    .toList()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("type", a.type());
                item.put("uri", a.uri());
                item.put("hash", a.hash() == null ? "" : a.hash());
                arts.add(item);
            }
        }
        root.put("artifacts", arts);
        try {
            return sha256(objectMapper.writeValueAsString(root));
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RELEASE_MANIFEST_CHANGED", "Failed to fingerprint release content");
        }
    }

    public record ArtifactFingerprint(String type, String uri, String hash) {
    }

    private static List<String> refs(List<ReleaseContentEntity> contents, ReleaseContentType type) {
        return contents.stream()
                .filter(c -> c.getContentType() == type)
                .map(ReleaseContentEntity::getReferenceId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> sortedUuids(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).map(UUID::toString).sorted().toList();
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
