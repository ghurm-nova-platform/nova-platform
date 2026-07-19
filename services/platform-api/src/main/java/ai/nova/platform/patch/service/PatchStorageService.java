package ai.nova.platform.patch.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.ArtifactReference;
import ai.nova.platform.patch.dto.PatchDtos.PatchFile;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.dto.PatchDtos.PatchStatistics;
import ai.nova.platform.patch.dto.PatchDtos.PatchValidation;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.GeneratedPatchEntity;
import ai.nova.platform.patch.entity.PatchArtifactEntity;
import ai.nova.platform.patch.entity.PatchResultEntity;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.repository.GeneratedPatchRepository;
import ai.nova.platform.patch.repository.PatchArtifactRepository;
import ai.nova.platform.patch.repository.PatchResultRepository;
import ai.nova.platform.patch.service.PatchDiffParser.FileDiff;
import ai.nova.platform.patch.service.PatchDiffParser.ParsedDiff;

/**
 * Persists generated patch text, statistics, and artifact references.
 * Never applies patches or mutates repositories.
 */
@Service
public class PatchStorageService {

    private final PatchResultRepository resultRepository;
    private final GeneratedPatchRepository patchFileRepository;
    private final PatchArtifactRepository artifactRepository;

    public PatchStorageService(
            PatchResultRepository resultRepository,
            GeneratedPatchRepository patchFileRepository,
            PatchArtifactRepository artifactRepository) {
        this.resultRepository = resultRepository;
        this.patchFileRepository = patchFileRepository;
        this.artifactRepository = artifactRepository;
    }

    @Transactional
    public PatchResult replaceResult(
            AgentOrchestrationTask task,
            List<GeneratedArtifactResponse> artifacts,
            ParsedPatchOutput parsed,
            ParsedDiff diff,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs) {
        resultRepository.deleteByTaskIdAndOrganizationId(task.getId(), task.getOrganizationId());
        return persistNewResult(task, artifacts, parsed, diff, tokensUsed, model, provider, generationTimeMs);
    }

    /**
     * Appends a new PatchResult without deleting prior rows.
     * Used by Repair Agent so previous patches remain immutable history.
     */
    @Transactional
    public PatchResult appendResult(
            AgentOrchestrationTask task,
            List<GeneratedArtifactResponse> artifacts,
            ParsedPatchOutput parsed,
            ParsedDiff diff,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs) {
        return persistNewResult(task, artifacts, parsed, diff, tokensUsed, model, provider, generationTimeMs);
    }

    private PatchResult persistNewResult(
            AgentOrchestrationTask task,
            List<GeneratedArtifactResponse> artifacts,
            ParsedPatchOutput parsed,
            ParsedDiff diff,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs) {
        Instant now = Instant.now();
        UUID resultId = UUID.randomUUID();
        String patchContent = diff.normalizedPatch();
        PatchResultEntity result = new PatchResultEntity(
                resultId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getRunId(),
                task.getId(),
                parsed.summary().trim(),
                PatchStatus.VALID,
                diff.filesChanged(),
                diff.insertions(),
                diff.deletions(),
                patchContent.length(),
                patchContent,
                "Unified diff validated",
                tokensUsed,
                model,
                provider,
                generationTimeMs,
                now);
        resultRepository.save(result);

        List<PatchFile> files = new ArrayList<>();
        for (FileDiff file : diff.files()) {
            UUID fileId = UUID.randomUUID();
            GeneratedPatchEntity entity = new GeneratedPatchEntity(
                    fileId,
                    resultId,
                    task.getOrganizationId(),
                    file.path(),
                    file.oldPath(),
                    file.newPath(),
                    file.changeType(),
                    file.insertions(),
                    file.deletions(),
                    file.excerpt(),
                    now);
            patchFileRepository.save(entity);
            files.add(toFile(entity));
        }

        for (GeneratedArtifactResponse artifact : artifacts) {
            artifactRepository.save(new PatchArtifactEntity(
                    UUID.randomUUID(),
                    resultId,
                    task.getOrganizationId(),
                    artifact.id(),
                    artifact.path(),
                    artifact.filename(),
                    artifact.language().name(),
                    artifact.sha256(),
                    now));
        }

        List<ArtifactReference> refs = artifactRepository.findByPatchResultIdOrderByPathAsc(resultId).stream()
                .map(PatchStorageService::toArtifact)
                .toList();
        return toResult(result, files, refs);
    }

    @Transactional(readOnly = true)
    public PatchResult findLatest(UUID taskId, UUID organizationId) {
        return resultRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(result -> {
                    List<PatchFile> files = patchFileRepository
                            .findByPatchResultIdOrderByPathAsc(result.getId())
                            .stream()
                            .map(PatchStorageService::toFile)
                            .toList();
                    List<ArtifactReference> refs = artifactRepository
                            .findByPatchResultIdOrderByPathAsc(result.getId())
                            .stream()
                            .map(PatchStorageService::toArtifact)
                            .toList();
                    return toResult(result, files, refs);
                })
                .orElse(null);
    }

    private static PatchResult toResult(
            PatchResultEntity result, List<PatchFile> files, List<ArtifactReference> artifacts) {
        PatchStatistics statistics = new PatchStatistics(
                result.getFilesChanged(),
                result.getInsertions(),
                result.getDeletions(),
                result.getPatchSize());
        PatchValidation validation = new PatchValidation(
                result.getStatus() == PatchStatus.VALID,
                result.getValidationMessage() == null ? "" : result.getValidationMessage());
        return new PatchResult(
                result.getId(),
                result.getTaskId(),
                result.getRunId(),
                result.getProjectId(),
                result.getSummary(),
                result.getStatus(),
                statistics,
                result.getPatchContent(),
                files,
                artifacts,
                validation,
                result.getTokensUsed(),
                result.getModel(),
                result.getProvider(),
                result.getGenerationTimeMs(),
                result.getCreatedAt());
    }

    private static PatchFile toFile(GeneratedPatchEntity entity) {
        return new PatchFile(
                entity.getId(),
                entity.getPath(),
                entity.getOldPath(),
                entity.getNewPath(),
                entity.getChangeType(),
                entity.getInsertions(),
                entity.getDeletions(),
                entity.getPatchExcerpt());
    }

    private static ArtifactReference toArtifact(PatchArtifactEntity entity) {
        return new ArtifactReference(
                entity.getArtifactId(),
                entity.getPath(),
                entity.getFilename(),
                entity.getLanguage(),
                entity.getSha256());
    }
}
