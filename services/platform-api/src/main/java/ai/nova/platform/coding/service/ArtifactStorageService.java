package ai.nova.platform.coding.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.GeneratedArtifact;
import ai.nova.platform.coding.repository.GeneratedArtifactRepository;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.web.error.ApiException;

/**
 * Persists generated artifacts with SHA-256 digests. Does not write to git or the filesystem.
 * Future phases may commit these records into repositories.
 */
@Service
public class ArtifactStorageService {

    private final GeneratedArtifactRepository artifactRepository;
    private final CodingArtifactValidator validator;

    public ArtifactStorageService(
            GeneratedArtifactRepository artifactRepository, CodingArtifactValidator validator) {
        this.artifactRepository = artifactRepository;
        this.validator = validator;
    }

    @Transactional
    public List<GeneratedArtifactResponse> replaceArtifacts(
            AgentOrchestrationTask task,
            List<GeneratedArtifactDraft> drafts,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs) {
        artifactRepository.deleteByTaskIdAndOrganizationId(task.getId(), task.getOrganizationId());
        Instant now = Instant.now();
        List<GeneratedArtifact> saved = new ArrayList<>();
        for (GeneratedArtifactDraft draft : drafts) {
            String path = validator.normalizePath(draft.path());
            String filename = draft.filename() == null || draft.filename().isBlank()
                    ? filenameFromPath(path)
                    : draft.filename().trim();
            GeneratedArtifact entity = new GeneratedArtifact(
                    UUID.randomUUID(),
                    task.getOrganizationId(),
                    task.getProjectId(),
                    task.getRunId(),
                    task.getId(),
                    draft.type(),
                    draft.language(),
                    path,
                    filename,
                    draft.content(),
                    sha256(draft.content()),
                    tokensUsed,
                    model,
                    provider,
                    generationTimeMs,
                    now);
            saved.add(artifactRepository.save(entity));
        }
        return saved.stream().map(ArtifactStorageService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GeneratedArtifactResponse> listByTask(UUID taskId, UUID organizationId) {
        return artifactRepository.findByTaskIdAndOrganizationIdOrderByPathAsc(taskId, organizationId).stream()
                .map(ArtifactStorageService::toResponse)
                .toList();
    }

    static GeneratedArtifactResponse toResponse(GeneratedArtifact artifact) {
        return new GeneratedArtifactResponse(
                artifact.getId(),
                artifact.getOrganizationId(),
                artifact.getProjectId(),
                artifact.getRunId(),
                artifact.getTaskId(),
                artifact.getArtifactType(),
                artifact.getLanguage(),
                artifact.getPath(),
                artifact.getFilename(),
                artifact.getContent(),
                artifact.getSha256(),
                artifact.getTokensUsed(),
                artifact.getModel(),
                artifact.getProvider(),
                artifact.getGenerationTimeMs(),
                artifact.getCreatedAt());
    }

    private static String filenameFromPath(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "CODING_HASH_FAILED", "Unable to compute artifact digest");
        }
    }
}
