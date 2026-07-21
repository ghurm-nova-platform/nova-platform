package ai.nova.platform.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.dashboard.entity.PipelineStageCode;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardMeta(
            UUID organizationId,
            UUID projectId,
            Instant generatedAt,
            Instant cacheExpiresAt,
            int refreshRateSeconds,
            boolean fromCache) {}

    public record DashboardKpis(
            double releaseSuccessRate,
            double deploymentSuccessRate,
            double pipelineSuccessRate,
            double approvalSlaComplianceRate,
            double ciPassRate,
            double rollbackReadinessRate,
            long avgReleaseDurationMs,
            long avgDeploymentDurationMs,
            long avgPipelineStageDurationMs,
            long avgApprovalWaitMs,
            long avgCiDurationMs,
            long avgRollbackPlanDurationMs) {}

    public record OverviewSection(
            long projectCount,
            long agentCount,
            long activeRunCount,
            long totalRunCount,
            long releaseCount,
            long deploymentCount,
            long executionCount,
            long environmentCount,
            long auditEventCount,
            long pendingApprovalCount,
            long failedCiCount,
            long rollbackReadyCount,
            DashboardKpis kpis) {}

    public record PipelineStageSnapshot(
            PipelineStageCode stage,
            String label,
            long current,
            long waiting,
            long failed,
            long success,
            long avgDurationMs) {}

    public record PipelineSection(List<PipelineStageSnapshot> stages, long totalActiveTasks) {}

    public record DeploymentExecutionSnapshot(
            UUID id,
            UUID projectId,
            UUID environmentId,
            String provider,
            String environmentCode,
            String status,
            String currentStage,
            String currentStep,
            Long durationMs,
            int progressPercent,
            String verifyStatus,
            Instant startedAt) {}

    public record DeploymentsSection(
            List<DeploymentExecutionSnapshot> running,
            long totalRunning,
            long totalCompleted,
            long totalFailed) {}

    public record ReleaseSnapshot(
            UUID id,
            UUID projectId,
            String releaseName,
            String semanticVersion,
            String status,
            Instant createdAt,
            Instant publishedAt) {}

    public record ReleasesSection(
            long published,
            long ready,
            long blocked,
            long pendingApproval,
            long policyFailures,
            long rollbackReady,
            List<ReleaseSnapshot> recent) {}

    public record EnvironmentBucketSnapshot(
            String bucket,
            long environmentCount,
            long healthy,
            long degraded,
            long unavailable,
            long runningExecutions,
            long recentDeployments) {}

    public record EnvironmentItemSnapshot(
            UUID id,
            UUID projectId,
            String code,
            String name,
            String environmentType,
            String status,
            String health,
            long runningExecutions,
            long recentDeployments,
            Map<String, String> labels) {}

    public record EnvironmentsSection(
            List<EnvironmentBucketSnapshot> buckets,
            List<EnvironmentItemSnapshot> environments) {}

    public record AuditSection(List<AuditEvent> events, long total) {}

    public record ApprovalQueueItem(
            UUID taskId,
            UUID runId,
            UUID projectId,
            String displayName,
            String status,
            Long slaRemainingMs,
            boolean expired,
            boolean blocked,
            Instant waitingSince) {}

    public record ApprovalsSection(
            long waiting,
            long expired,
            long blocked,
            long slaBreaches,
            List<ApprovalQueueItem> queue) {}

    public record CiPipelineSnapshot(
            UUID taskId,
            UUID projectId,
            String provider,
            String repository,
            String branch,
            String overallStatus,
            Long durationMs,
            Instant completedAt,
            boolean failed) {}

    public record CiSection(
            List<CiPipelineSnapshot> recentPipelines,
            long failedBuilds,
            long repairRequests,
            long queueDepth,
            long avgDurationMs) {}

    public record RollbackSnapshot(
            UUID id,
            UUID projectId,
            String currentVersion,
            String targetVersion,
            String environmentCode,
            String status,
            Long durationMs,
            Instant createdAt) {}

    public record RollbacksSection(
            long ready,
            long executed,
            long failed,
            double coveragePercent,
            long avgDurationMs,
            List<RollbackSnapshot> recent) {}

    public record CostProviderUsage(String provider, double estimatedCost, long operationCount) {}

    public record CostSection(
            double estimatedTotalCost,
            List<CostProviderUsage> providerUsage,
            double futureLlmCostEstimate,
            String note) {}

    public record DashboardSnapshot(
            DashboardMeta meta,
            OverviewSection overview,
            PipelineSection pipeline,
            DeploymentsSection deployments,
            ReleasesSection releases,
            EnvironmentsSection environments,
            AuditSection audit,
            ApprovalsSection approvals,
            CiSection ci,
            RollbacksSection rollbacks,
            CostSection cost) {}

    public record DashboardRefreshResponse(Instant refreshedAt, Instant cacheExpiresAt) {}

    public record DashboardConfigResponse(boolean enabled, int refreshRateSeconds, int cacheTtlSeconds) {}
}
