package ai.nova.platform.review.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.review.dto.ReviewDtos.ArtifactReview;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFindingDraft;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.entity.ReviewFindingEntity;
import ai.nova.platform.review.entity.ReviewResultEntity;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.review.entity.ReviewedArtifactEntity;
import ai.nova.platform.review.repository.ReviewFindingRepository;
import ai.nova.platform.review.repository.ReviewResultRepository;
import ai.nova.platform.review.repository.ReviewedArtifactRepository;

/**
 * Persists review score, approval, findings, and artifact references.
 * Does not modify generated artifact content.
 */
@Service
public class ReviewStorageService {

    private final ReviewResultRepository resultRepository;
    private final ReviewFindingRepository findingRepository;
    private final ReviewedArtifactRepository reviewedArtifactRepository;

    public ReviewStorageService(
            ReviewResultRepository resultRepository,
            ReviewFindingRepository findingRepository,
            ReviewedArtifactRepository reviewedArtifactRepository) {
        this.resultRepository = resultRepository;
        this.findingRepository = findingRepository;
        this.reviewedArtifactRepository = reviewedArtifactRepository;
    }

    @Transactional
    public ReviewResult replaceReview(
            AgentOrchestrationTask task,
            List<GeneratedArtifactResponse> artifacts,
            ParsedReviewOutput parsed,
            Long tokensUsed,
            String model,
            String provider,
            Long reviewTimeMs) {
        resultRepository.deleteByTaskIdAndOrganizationId(task.getId(), task.getOrganizationId());

        Instant now = Instant.now();
        UUID resultId = UUID.randomUUID();
        ReviewResultEntity result = new ReviewResultEntity(
                resultId,
                task.getOrganizationId(),
                task.getProjectId(),
                task.getRunId(),
                task.getId(),
                parsed.summary(),
                parsed.score(),
                parsed.approved(),
                tokensUsed,
                model,
                provider,
                reviewTimeMs,
                now);
        resultRepository.save(result);

        Map<String, GeneratedArtifactResponse> byPath = new LinkedHashMap<>();
        for (GeneratedArtifactResponse artifact : artifacts) {
            byPath.put(artifact.path(), artifact);
            reviewedArtifactRepository.save(new ReviewedArtifactEntity(
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

        List<ReviewFinding> findings = new ArrayList<>();
        for (ReviewFindingDraft draft : parsed.findings()) {
            GeneratedArtifactResponse matched =
                    draft.artifactPath() == null ? null : byPath.get(draft.artifactPath());
            ReviewFindingEntity entity = new ReviewFindingEntity(
                    UUID.randomUUID(),
                    resultId,
                    task.getOrganizationId(),
                    draft.severity(),
                    draft.category(),
                    draft.title().trim(),
                    draft.description().trim(),
                    draft.recommendation().trim(),
                    matched != null ? matched.id() : null,
                    draft.artifactPath(),
                    now);
            findingRepository.save(entity);
            findings.add(toFinding(entity));
        }

        List<ArtifactReview> reviewed = reviewedArtifactRepository
                .findByReviewResultIdOrderByPathAsc(resultId)
                .stream()
                .map(ReviewStorageService::toArtifactReview)
                .toList();

        return toResult(result, findings, reviewed);
    }

    @Transactional(readOnly = true)
    public ReviewResult findLatest(UUID taskId, UUID organizationId) {
        return resultRepository
                .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, organizationId)
                .map(result -> {
                    List<ReviewFinding> findings = findingRepository
                            .findByReviewResultIdOrderBySeverityDescTitleAsc(result.getId())
                            .stream()
                            .map(ReviewStorageService::toFinding)
                            .toList();
                    List<ArtifactReview> reviewed = reviewedArtifactRepository
                            .findByReviewResultIdOrderByPathAsc(result.getId())
                            .stream()
                            .map(ReviewStorageService::toArtifactReview)
                            .toList();
                    return toResult(result, findings, reviewed);
                })
                .orElse(null);
    }

    private static ReviewResult toResult(
            ReviewResultEntity result, List<ReviewFinding> findings, List<ArtifactReview> reviewed) {
        return new ReviewResult(
                result.getId(),
                result.getTaskId(),
                result.getRunId(),
                result.getProjectId(),
                result.getSummary(),
                result.getScore(),
                result.isApproved(),
                findings,
                reviewed,
                severityCounts(findings),
                result.getTokensUsed(),
                result.getModel(),
                result.getProvider(),
                result.getReviewTimeMs(),
                result.getCreatedAt(),
                true);
    }

    private static Map<String, Long> severityCounts(List<ReviewFinding> findings) {
        Map<ReviewSeverity, Long> counts = new EnumMap<>(ReviewSeverity.class);
        for (ReviewSeverity severity : ReviewSeverity.values()) {
            counts.put(severity, 0L);
        }
        for (ReviewFinding finding : findings) {
            counts.merge(finding.severity(), 1L, Long::sum);
        }
        Map<String, Long> out = new LinkedHashMap<>();
        counts.forEach((k, v) -> out.put(k.name(), v));
        return out;
    }

    private static ReviewFinding toFinding(ReviewFindingEntity entity) {
        return new ReviewFinding(
                entity.getId(),
                entity.getSeverity(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getRecommendation(),
                entity.getArtifactId(),
                entity.getArtifactPath());
    }

    private static ArtifactReview toArtifactReview(ReviewedArtifactEntity entity) {
        return new ArtifactReview(
                entity.getArtifactId(),
                entity.getPath(),
                entity.getFilename(),
                entity.getLanguage(),
                entity.getSha256());
    }
}
