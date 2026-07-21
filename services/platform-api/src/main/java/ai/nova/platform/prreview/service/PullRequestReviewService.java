package ai.nova.platform.prreview.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.prreview.config.PrReviewProperties;
import ai.nova.platform.prreview.dto.PrReviewDtos.ExportPayload;
import ai.nova.platform.prreview.dto.PrReviewDtos.FindingView;
import ai.nova.platform.prreview.dto.PrReviewDtos.KnowledgeReferenceView;
import ai.nova.platform.prreview.dto.PrReviewDtos.PrReviewConfigResponse;
import ai.nova.platform.prreview.dto.PrReviewDtos.RecommendationView;
import ai.nova.platform.prreview.dto.PrReviewDtos.RerunRequest;
import ai.nova.platform.prreview.dto.PrReviewDtos.ReviewRunDetail;
import ai.nova.platform.prreview.dto.PrReviewDtos.ReviewRunSummary;
import ai.nova.platform.prreview.dto.PrReviewDtos.RiskScoreView;
import ai.nova.platform.prreview.dto.PrReviewDtos.RunRequest;
import ai.nova.platform.prreview.entity.PrReviewFindingEntity;
import ai.nova.platform.prreview.entity.PrReviewRecommendationEntity;
import ai.nova.platform.prreview.entity.PrReviewRunEntity;
import ai.nova.platform.prreview.entity.ReviewRunStatus;
import ai.nova.platform.prreview.repository.PrReviewFindingRepository;
import ai.nova.platform.prreview.repository.PrReviewRecommendationRepository;
import ai.nova.platform.prreview.repository.PrReviewRunRepository;
import ai.nova.platform.prreview.security.PullRequestReviewAuthorizationService;
import ai.nova.platform.prreview.service.ReviewAggregator.AggregationResult;
import ai.nova.platform.prreview.service.ReviewRecommendationService.RecommendationDraft;
import ai.nova.platform.prreview.service.ReviewRiskScoreService.ScoreBucket;
import ai.nova.platform.pullrequest.repository.PullRequestOperationRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class PullRequestReviewService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestReviewService.class);
    private static final float PDF_FONT_SIZE = 10f;
    private static final float PDF_LEADING = 14f;
    private static final float PDF_MARGIN = 50f;

    private final PrReviewProperties properties;
    private final PullRequestReviewAuthorizationService authorizationService;
    private final PrReviewRunRepository runRepository;
    private final PrReviewFindingRepository findingRepository;
    private final PrReviewRecommendationRepository recommendationRepository;
    private final ProjectRepository projectRepository;
    private final PullRequestOperationRepository pullRequestOperationRepository;
    private final ReviewAggregator reviewAggregator;
    private final AuditRecordingSupport auditRecordingSupport;
    private final PrReviewMetrics metrics;
    private final ObjectMapper objectMapper;

    public PullRequestReviewService(
            PrReviewProperties properties,
            PullRequestReviewAuthorizationService authorizationService,
            PrReviewRunRepository runRepository,
            PrReviewFindingRepository findingRepository,
            PrReviewRecommendationRepository recommendationRepository,
            ProjectRepository projectRepository,
            PullRequestOperationRepository pullRequestOperationRepository,
            ReviewAggregator reviewAggregator,
            AuditRecordingSupport auditRecordingSupport,
            PrReviewMetrics metrics,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.runRepository = runRepository;
        this.findingRepository = findingRepository;
        this.recommendationRepository = recommendationRepository;
        this.projectRepository = projectRepository;
        this.pullRequestOperationRepository = pullRequestOperationRepository;
        this.reviewAggregator = reviewAggregator;
        this.auditRecordingSupport = auditRecordingSupport;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    public PrReviewConfigResponse config(AuthenticatedUser user) {
        authorizationService.requireRead(user);
        return new PrReviewConfigResponse(
                properties.isEnabled(),
                properties.getMaxDiffCharacters(),
                properties.getDefaultLimit(),
                properties.getMaxFindings(),
                properties.getMaxRecommendations(),
                properties.isParallelAnalysis(),
                properties.isExportEnabled(),
                properties.getCache().isEnabled(),
                properties.getCache().getTtlSeconds());
    }

    @Transactional
    public ReviewRunDetail run(RunRequest request, AuthenticatedUser user) {
        authorizationService.requireRun(user);
        requireEnabled();
        validateProject(user, request.projectId());
        validatePullRequestOperation(user, request.pullRequestOperationId());
        guardAgainstConcurrentRun(user.getOrganizationId(), request.pullRequestOperationId(), request.pullRequestNumber());

        String diff = capDiff(request.diffContent());
        Instant now = Instant.now();
        PrReviewRunEntity run = new PrReviewRunEntity(
                UUID.randomUUID(),
                user.getOrganizationId(),
                request.projectId(),
                request.pullRequestOperationId(),
                request.pullRequestNumber(),
                request.pullRequestTitle(),
                request.repositoryRef(),
                request.sourceBranch(),
                request.targetBranch(),
                ReviewRunStatus.RUNNING,
                diff,
                user.getUserId(),
                now);
        run.setStartedAt(now);
        run.setCommitSha(request.commitSha());
        run.setChangedFilesJson(toJson(request.changedFiles() == null ? List.of() : request.changedFiles()));
        runRepository.save(run);
        log.info("PR review started id={} projectId={}", run.getId(), run.getProjectId());

        try {
            ReviewContext context = new ReviewContext(diff, request.changedFiles(), request.commitSha());
            AggregationResult aggregation = reviewAggregator.analyze(context, user, request.projectId());
            persistAggregation(run, aggregation, now);
            audit(user, run, AuditAction.VALIDATE, AuditResult.SUCCESS, Map.of("action", "RUN"));
            return toDetail(run, false);
        } catch (RuntimeException ex) {
            Instant failedAt = Instant.now();
            run.setStatus(ReviewRunStatus.FAILED);
            run.setCompletedAt(failedAt);
            run.setUpdatedAt(failedAt);
            run.setSummary("Review failed: " + ex.getMessage());
            runRepository.save(run);
            metrics.recordFailure();
            log.error("PR review failed id={}", run.getId(), ex);
            audit(user, run, AuditAction.VALIDATE, AuditResult.FAILURE, Map.of("action", "RUN", "error", ex.getMessage()));
            throw ex;
        }
    }

    @Transactional
    public ReviewRunDetail rerun(UUID previousRunId, RerunRequest request, AuthenticatedUser user) {
        authorizationService.requireRun(user);
        requireEnabled();
        PrReviewRunEntity previous = requireRun(previousRunId, user.getOrganizationId());
        String diffContent = request != null && request.diffContent() != null && !request.diffContent().isBlank()
                ? request.diffContent()
                : previous.getDiffExcerpt();
        if (diffContent == null || diffContent.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_REVIEW_INVALID_REQUEST", "Diff content is required");
        }
        List<String> changedFiles = request != null && request.changedFiles() != null
                ? request.changedFiles()
                : readStringList(previous.getChangedFilesJson());
        String commitSha = request != null && request.commitSha() != null
                ? request.commitSha()
                : previous.getCommitSha();
        RunRequest runRequest = new RunRequest(
                previous.getProjectId(),
                previous.getPullRequestOperationId(),
                previous.getPullRequestNumber(),
                previous.getPullRequestTitle(),
                previous.getRepositoryRef(),
                previous.getSourceBranch(),
                previous.getTargetBranch(),
                commitSha,
                changedFiles,
                diffContent);
        ReviewRunDetail detail = run(runRequest, user);
        audit(
                user,
                requireRun(detail.id(), user.getOrganizationId()),
                AuditAction.VALIDATE,
                AuditResult.SUCCESS,
                Map.of("action", "RERUN", "previousRunId", previousRunId.toString()));
        return detail;
    }

    @Transactional(readOnly = true)
    public List<ReviewRunSummary> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        List<PrReviewRunEntity> runs = projectId == null
                ? runRepository.findByOrganizationIdOrderByCreatedAtDesc(user.getOrganizationId())
                : runRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        user.getOrganizationId(), projectId);
        return limit(runs).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ReviewRunDetail get(UUID id, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        PrReviewRunEntity run = requireRun(id, user.getOrganizationId());
        audit(user, run, AuditAction.ACCESS, AuditResult.SUCCESS, Map.of("action", "VIEW"));
        return toDetail(run, false);
    }

    @Transactional(readOnly = true)
    public List<ReviewRunSummary> history(
            UUID projectId, UUID pullRequestOperationId, Integer pullRequestNumber, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        List<PrReviewRunEntity> runs;
        if (pullRequestOperationId != null) {
            runs = runRepository.findByOrganizationIdAndPullRequestOperationIdOrderByCreatedAtDesc(
                    user.getOrganizationId(), pullRequestOperationId);
        } else if (pullRequestNumber != null) {
            runs = runRepository.findByOrganizationIdAndPullRequestNumberOrderByCreatedAtDesc(
                    user.getOrganizationId(), pullRequestNumber);
        } else if (projectId != null) {
            runs = runRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                    user.getOrganizationId(), projectId);
        } else {
            runs = runRepository.findByOrganizationIdOrderByCreatedAtDesc(user.getOrganizationId());
        }
        return limit(runs).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<FindingView> findings(UUID runId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        requireRun(runId, user.getOrganizationId());
        return findingRepository
                .findByReviewRunIdAndOrganizationIdOrderByCreatedAtAsc(runId, user.getOrganizationId())
                .stream()
                .map(this::toFindingView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationView> recommendations(UUID runId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        requireRun(runId, user.getOrganizationId());
        return recommendationRepository
                .findByReviewRunIdAndOrganizationIdOrderByCreatedAtAsc(runId, user.getOrganizationId())
                .stream()
                .map(this::toRecommendationView)
                .toList();
    }

    @Transactional(readOnly = true)
    public RiskScoreView riskScore(UUID runId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        PrReviewRunEntity run = requireRun(runId, user.getOrganizationId());
        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        categoryScores.put("Architecture", run.getArchitectureScore());
        categoryScores.put("Security", run.getSecurityScore());
        categoryScores.put("Performance", run.getPerformanceScore());
        categoryScores.put("Quality", run.getQualityScore());
        categoryScores.put("Testing", run.getTestingScore());
        categoryScores.put("Documentation", run.getDocumentationScore());
        return new RiskScoreView(run.getId(), run.getOverallScore(), run.getRiskScore(), run.getResult(), categoryScores);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeReferenceView> knowledge(UUID runId, AuthenticatedUser user) {
        authorizationService.requireRead(user);
        requireEnabled();
        requireRun(runId, user.getOrganizationId());
        return findingRepository
                .findByReviewRunIdAndOrganizationIdOrderByCreatedAtAsc(runId, user.getOrganizationId())
                .stream()
                .map(finding -> {
                    List<UUID> ids = readUuidList(finding.getKnowledgeDocumentIdsJson());
                    return new KnowledgeReferenceView(
                            finding.getId(), finding.getTitle(), finding.getCategory(), ids);
                })
                .filter(view -> view.knowledgeDocumentIds() != null && !view.knowledgeDocumentIds().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportPayload export(UUID runId, String format, AuthenticatedUser user) {
        authorizationService.requireExport(user);
        requireEnabled();
        if (!properties.isExportEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PR_REVIEW_CONFIGURATION_ERROR",
                    "PR review export is disabled");
        }
        ReviewRunDetail detail = toDetail(requireRun(runId, user.getOrganizationId()), true);
        String normalized = format == null ? "markdown" : format.trim().toLowerCase(Locale.ROOT);
        ExportPayload payload = switch (normalized) {
            case "markdown", "md" -> exportMarkdown(detail);
            case "json" -> exportJson(detail);
            case "pdf" -> exportPdf(detail);
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_REVIEW_INVALID_REQUEST", "Unsupported export format: " + format);
        };
        metrics.recordExport();
        log.info("PR review exported id={} format={}", runId, normalized);
        audit(
                user,
                requireRun(runId, user.getOrganizationId()),
                AuditAction.PUBLISH,
                AuditResult.SUCCESS,
                Map.of("action", "EXPORT", "format", normalized));
        return payload;
    }

    private void persistAggregation(PrReviewRunEntity run, AggregationResult aggregation, Instant startedAt) {
        Instant completedAt = Instant.now();
        run.setArchitectureScore(aggregation.scores().getOrDefault(ScoreBucket.ARCHITECTURE, 100));
        run.setSecurityScore(aggregation.scores().getOrDefault(ScoreBucket.SECURITY, 100));
        run.setPerformanceScore(aggregation.scores().getOrDefault(ScoreBucket.PERFORMANCE, 100));
        run.setQualityScore(aggregation.scores().getOrDefault(ScoreBucket.QUALITY, 100));
        run.setTestingScore(aggregation.scores().getOrDefault(ScoreBucket.TESTING, 100));
        run.setDocumentationScore(aggregation.scores().getOrDefault(ScoreBucket.DOCUMENTATION, 100));
        run.setOverallScore(aggregation.overallScore());
        run.setRiskScore(aggregation.riskScore());
        run.setResult(aggregation.result());
        run.setSummary(aggregation.summary());
        run.setStatus(ReviewRunStatus.COMPLETED);
        run.setCompletedAt(completedAt);
        run.setUpdatedAt(completedAt);
        if (run.getStartedAt() == null) {
            run.setStartedAt(startedAt);
        }
        runRepository.save(run);

        Map<FindingDraft, UUID> findingIds = new LinkedHashMap<>();
        for (FindingDraft draft : aggregation.findings()) {
            PrReviewFindingEntity finding = new PrReviewFindingEntity(
                    UUID.randomUUID(),
                    run.getId(),
                    run.getOrganizationId(),
                    draft.category(),
                    draft.severity(),
                    draft.title(),
                    draft.description(),
                    draft.recommendation(),
                    draft.filePath(),
                    draft.lineHint(),
                    toJson(draft.references()),
                    toJson(draft.knowledgeDocumentIds()),
                    draft.ruleCode(),
                    draft.evidenceExcerpt(),
                    completedAt);
            findingRepository.save(finding);
            findingIds.put(draft, finding.getId());
        }

        for (RecommendationDraft draft : aggregation.recommendations()) {
            UUID findingId = draft.findingId() != null
                    ? draft.findingId()
                    : findingIds.get(draft.sourceFinding());
            recommendationRepository.save(new PrReviewRecommendationEntity(
                    UUID.randomUUID(),
                    run.getId(),
                    run.getOrganizationId(),
                    findingId,
                    draft.priority(),
                    draft.title(),
                    draft.description(),
                    toJson(draft.knowledgeDocumentIds()),
                    completedAt));
        }
    }

    private void guardAgainstConcurrentRun(
            UUID organizationId, UUID pullRequestOperationId, Integer pullRequestNumber) {
        List<PrReviewRunEntity> candidates;
        if (pullRequestOperationId != null) {
            candidates = runRepository.findByOrganizationIdAndPullRequestOperationIdOrderByCreatedAtDesc(
                    organizationId, pullRequestOperationId);
        } else if (pullRequestNumber != null) {
            candidates = runRepository.findByOrganizationIdAndPullRequestNumberOrderByCreatedAtDesc(
                    organizationId, pullRequestNumber);
        } else {
            return;
        }
        boolean running = candidates.stream().anyMatch(run -> run.getStatus() == ReviewRunStatus.RUNNING);
        if (running) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PR_REVIEW_ALREADY_RUNNING",
                    "A review is already running for this pull request");
        }
    }

    private String capDiff(String diffContent) {
        if (diffContent == null || diffContent.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "PR_REVIEW_INVALID_REQUEST", "Diff content is required");
        }
        int max = properties.getMaxDiffCharacters();
        if (diffContent.length() <= max) {
            return diffContent;
        }
        return diffContent.substring(0, max);
    }

    private void validateProject(AuthenticatedUser user, UUID projectId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void validatePullRequestOperation(AuthenticatedUser user, UUID pullRequestOperationId) {
        if (pullRequestOperationId == null) {
            return;
        }
        pullRequestOperationRepository
                .findById(pullRequestOperationId)
                .filter(op -> user.getOrganizationId().equals(op.getOrganizationId()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PULL_REQUEST_NOT_FOUND", "Pull request operation not found"));
    }

    private PrReviewRunEntity requireRun(UUID id, UUID organizationId) {
        return runRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PR_REVIEW_NOT_FOUND", "PR review run not found"));
    }

    private List<PrReviewRunEntity> limit(List<PrReviewRunEntity> runs) {
        int limit = Math.max(1, properties.getDefaultLimit());
        if (runs.size() <= limit) {
            return runs;
        }
        return runs.subList(0, limit);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PR_REVIEW_CONFIGURATION_ERROR",
                    "PR review engine is disabled");
        }
    }

    private void audit(
            AuthenticatedUser user,
            PrReviewRunEntity run,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>(details);
        payload.put("overallScore", run.getOverallScore());
        payload.put("riskScore", run.getRiskScore());
        payload.put("result", run.getResult() == null ? null : run.getResult().name());
        auditRecordingSupport.recordDomainEvent(
                user,
                run.getProjectId(),
                AuditEntityType.PR_REVIEW,
                run.getId(),
                run.getPullRequestTitle() == null ? "PR Review " + run.getId() : run.getPullRequestTitle(),
                action,
                result,
                AuditSource.PR_REVIEW,
                payload);
    }

    private ReviewRunSummary toSummary(PrReviewRunEntity run) {
        return new ReviewRunSummary(
                run.getId(),
                run.getOrganizationId(),
                run.getProjectId(),
                run.getPullRequestOperationId(),
                run.getPullRequestNumber(),
                run.getPullRequestTitle(),
                run.getRepositoryRef(),
                run.getSourceBranch(),
                run.getTargetBranch(),
                run.getCommitSha(),
                run.getStatus(),
                run.getResult(),
                run.getOverallScore(),
                run.getRiskScore(),
                run.getArchitectureScore(),
                run.getSecurityScore(),
                run.getPerformanceScore(),
                run.getQualityScore(),
                run.getTestingScore(),
                run.getDocumentationScore(),
                run.getSummary(),
                run.getCreatedBy(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCreatedAt(),
                run.getUpdatedAt());
    }

    private ReviewRunDetail toDetail(PrReviewRunEntity run, boolean includeDiff) {
        List<FindingView> findings = findingRepository
                .findByReviewRunIdAndOrganizationIdOrderByCreatedAtAsc(run.getId(), run.getOrganizationId())
                .stream()
                .map(this::toFindingView)
                .toList();
        List<RecommendationView> recommendations = recommendationRepository
                .findByReviewRunIdAndOrganizationIdOrderByCreatedAtAsc(run.getId(), run.getOrganizationId())
                .stream()
                .map(this::toRecommendationView)
                .toList();
        return new ReviewRunDetail(
                run.getId(),
                run.getOrganizationId(),
                run.getProjectId(),
                run.getPullRequestOperationId(),
                run.getPullRequestNumber(),
                run.getPullRequestTitle(),
                run.getRepositoryRef(),
                run.getSourceBranch(),
                run.getTargetBranch(),
                run.getCommitSha(),
                readStringList(run.getChangedFilesJson()),
                run.getStatus(),
                run.getResult(),
                run.getOverallScore(),
                run.getRiskScore(),
                run.getArchitectureScore(),
                run.getSecurityScore(),
                run.getPerformanceScore(),
                run.getQualityScore(),
                run.getTestingScore(),
                run.getDocumentationScore(),
                run.getSummary(),
                includeDiff ? run.getDiffExcerpt() : null,
                run.getCreatedBy(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                findings,
                recommendations);
    }

    private FindingView toFindingView(PrReviewFindingEntity finding) {
        return new FindingView(
                finding.getId(),
                finding.getReviewRunId(),
                finding.getCategory(),
                finding.getSeverity(),
                finding.getTitle(),
                finding.getDescription(),
                finding.getRecommendation(),
                finding.getFilePath(),
                finding.getLineHint(),
                finding.getRuleCode(),
                finding.getEvidenceExcerpt(),
                readStringList(finding.getReferencesJson()),
                readUuidList(finding.getKnowledgeDocumentIdsJson()),
                finding.getCreatedAt());
    }

    private RecommendationView toRecommendationView(PrReviewRecommendationEntity recommendation) {
        return new RecommendationView(
                recommendation.getId(),
                recommendation.getReviewRunId(),
                recommendation.getFindingId(),
                recommendation.getPriority(),
                recommendation.getTitle(),
                recommendation.getDescription(),
                readUuidList(recommendation.getKnowledgeDocumentIdsJson()),
                recommendation.getCreatedAt());
    }

    private ExportPayload exportMarkdown(ReviewRunDetail detail) {
        StringBuilder builder = new StringBuilder();
        builder.append("# PR Review ").append(detail.id()).append("\n\n");
        builder.append("- Result: ").append(detail.result()).append("\n");
        builder.append("- Overall score: ").append(detail.overallScore()).append("\n");
        builder.append("- Risk score: ").append(detail.riskScore()).append("\n");
        builder.append("- Status: ").append(detail.status()).append("\n\n");
        if (detail.summary() != null) {
            builder.append(detail.summary()).append("\n\n");
        }
        builder.append("## Findings\n\n");
        for (FindingView finding : detail.findings()) {
            builder.append("### [")
                    .append(finding.severity())
                    .append("] ")
                    .append(finding.title())
                    .append("\n\n");
            builder.append(finding.description()).append("\n\n");
            builder.append("**Recommendation:** ").append(finding.recommendation()).append("\n\n");
        }
        builder.append("## Recommendations\n\n");
        for (RecommendationView recommendation : detail.recommendations()) {
            builder.append("- **")
                    .append(recommendation.priority())
                    .append("** ")
                    .append(recommendation.title())
                    .append(": ")
                    .append(recommendation.description())
                    .append("\n");
        }
        return new ExportPayload(
                builder.toString().getBytes(StandardCharsets.UTF_8),
                "text/markdown",
                "pr-review-" + detail.id() + ".md");
    }

    private ExportPayload exportJson(ReviewRunDetail detail) {
        try {
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(detail);
            return new ExportPayload(bytes, "application/json", "pr-review-" + detail.id() + ".json");
        } catch (JsonProcessingException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "PR_REVIEW_EXPORT_FAILED", "JSON export failed");
        }
    }

    private ExportPayload exportPdf(ReviewRunDetail detail) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(font, PDF_FONT_SIZE + 2);
                stream.newLineAtOffset(PDF_MARGIN, page.getMediaBox().getHeight() - PDF_MARGIN);
                stream.showText(truncate("PR Review " + detail.id(), 90));
                stream.endText();
                float y = page.getMediaBox().getHeight() - PDF_MARGIN - PDF_LEADING * 2;
                for (String line : wrapLines(buildPdfBody(detail), 95)) {
                    if (y < PDF_MARGIN) {
                        break;
                    }
                    stream.beginText();
                    stream.setFont(font, PDF_FONT_SIZE);
                    stream.newLineAtOffset(PDF_MARGIN, y);
                    stream.showText(line);
                    stream.endText();
                    y -= PDF_LEADING;
                }
            }
            pdf.save(out);
            return new ExportPayload(out.toByteArray(), "application/pdf", "pr-review-" + detail.id() + ".pdf");
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "PR_REVIEW_EXPORT_FAILED", "PDF export failed");
        }
    }

    private String buildPdfBody(ReviewRunDetail detail) {
        StringBuilder builder = new StringBuilder();
        builder.append("Result: ").append(detail.result()).append('\n');
        builder.append("Overall score: ").append(detail.overallScore()).append('\n');
        builder.append("Risk score: ").append(detail.riskScore()).append('\n');
        if (detail.summary() != null) {
            builder.append(detail.summary()).append('\n');
        }
        for (FindingView finding : detail.findings()) {
            builder.append('[')
                    .append(finding.severity())
                    .append("] ")
                    .append(finding.title())
                    .append(": ")
                    .append(finding.recommendation())
                    .append('\n');
        }
        return builder.toString();
    }

    private List<String> wrapLines(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        for (String raw : text.split("\\R")) {
            String remaining = sanitizePdfText(raw);
            while (remaining.length() > maxChars) {
                lines.add(remaining.substring(0, maxChars));
                remaining = remaining.substring(maxChars);
            }
            lines.add(remaining);
        }
        return lines;
    }

    private String sanitizePdfText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<UUID> readUuidList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
