package ai.nova.platform.prreview.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pr_review_runs")
public class PrReviewRunEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "pull_request_operation_id")
    private UUID pullRequestOperationId;

    @Column(name = "pull_request_number")
    private Integer pullRequestNumber;

    @Column(name = "pull_request_title", length = 500)
    private String pullRequestTitle;

    @Column(name = "repository_ref", length = 500)
    private String repositoryRef;

    @Column(name = "source_branch", length = 255)
    private String sourceBranch;

    @Column(name = "target_branch", length = 255)
    private String targetBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReviewRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 40)
    private ReviewResult result;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "architecture_score", nullable = false)
    private int architectureScore;

    @Column(name = "security_score", nullable = false)
    private int securityScore;

    @Column(name = "performance_score", nullable = false)
    private int performanceScore;

    @Column(name = "quality_score", nullable = false)
    private int qualityScore;

    @Column(name = "testing_score", nullable = false)
    private int testingScore;

    @Column(name = "documentation_score", nullable = false)
    private int documentationScore;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    @Column(name = "changed_files_json", columnDefinition = "TEXT")
    private String changedFilesJson;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "diff_excerpt", columnDefinition = "TEXT")
    private String diffExcerpt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PrReviewRunEntity() {
    }

    public PrReviewRunEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID pullRequestOperationId,
            Integer pullRequestNumber,
            String pullRequestTitle,
            String repositoryRef,
            String sourceBranch,
            String targetBranch,
            ReviewRunStatus status,
            String diffExcerpt,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.pullRequestOperationId = pullRequestOperationId;
        this.pullRequestNumber = pullRequestNumber;
        this.pullRequestTitle = pullRequestTitle;
        this.repositoryRef = repositoryRef;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.status = status;
        this.diffExcerpt = diffExcerpt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.overallScore = 0;
        this.architectureScore = 0;
        this.securityScore = 0;
        this.performanceScore = 0;
        this.qualityScore = 0;
        this.testingScore = 0;
        this.documentationScore = 0;
        this.riskScore = 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getPullRequestOperationId() {
        return pullRequestOperationId;
    }

    public Integer getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getPullRequestTitle() {
        return pullRequestTitle;
    }

    public String getRepositoryRef() {
        return repositoryRef;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public ReviewRunStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewRunStatus status) {
        this.status = status;
    }

    public ReviewResult getResult() {
        return result;
    }

    public void setResult(ReviewResult result) {
        this.result = result;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public int getArchitectureScore() {
        return architectureScore;
    }

    public void setArchitectureScore(int architectureScore) {
        this.architectureScore = architectureScore;
    }

    public int getSecurityScore() {
        return securityScore;
    }

    public void setSecurityScore(int securityScore) {
        this.securityScore = securityScore;
    }

    public int getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(int performanceScore) {
        this.performanceScore = performanceScore;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public int getTestingScore() {
        return testingScore;
    }

    public void setTestingScore(int testingScore) {
        this.testingScore = testingScore;
    }

    public int getDocumentationScore() {
        return documentationScore;
    }

    public void setDocumentationScore(int documentationScore) {
        this.documentationScore = documentationScore;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getChangedFilesJson() {
        return changedFilesJson;
    }

    public void setChangedFilesJson(String changedFilesJson) {
        this.changedFilesJson = changedFilesJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDiffExcerpt() {
        return diffExcerpt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
