package ai.nova.platform.dashboard.service;

import java.time.Duration;
import java.util.List;

import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardKpis;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.rollback.entity.RollbackStatus;

final class DashboardMetricsService {

    private DashboardMetricsService() {
    }

    static DashboardCounts countOverview(DashboardAggregationService.AggregationContext ctx) {
        long activeRuns = ctx.runs().stream().filter(r -> isActiveRun(r.getStatus())).count();
        long pendingApprovals = ctx.tasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.WAITING_APPROVAL || t.getStatus() == TaskStatus.BLOCKED)
                .count();
        long failedCi = ctx.tasks().stream()
                .filter(t -> DashboardAggregationService.resolveStage(t) == ai.nova.platform.dashboard.entity.PipelineStageCode.CI)
                .filter(t -> t.getStatus() == TaskStatus.FAILED)
                .count();
        long rollbackReady = ctx.rollbacks().stream()
                .filter(r -> r.getStatus() == RollbackStatus.READY)
                .count();
        return new DashboardCounts(
                ctx.projects().size(),
                ctx.agentCount(),
                activeRuns,
                ctx.runs().size(),
                ctx.releases().size(),
                ctx.deployments().size(),
                ctx.executions().size(),
                ctx.environments().size(),
                ctx.auditEventCount(),
                pendingApprovals,
                failedCi,
                rollbackReady);
    }

    static DashboardKpis computeKpis(DashboardAggregationService.AggregationContext ctx) {
        double releaseSuccess = rate(
                ctx.releases().stream().filter(r -> r.getStatus() == ReleaseStatus.PUBLISHED).count(),
                ctx.releases().size());
        double deploymentSuccess = rate(
                ctx.executions().stream().filter(e -> e.getStatus() == ExecutionStatus.COMPLETED).count(),
                ctx.executions().size());
        double pipelineSuccess = rate(
                ctx.tasks().stream().filter(t -> t.getStatus() == TaskStatus.SUCCEEDED).count(),
                ctx.tasks().size());
        double approvalSla = rate(
                ctx.tasks().stream()
                        .filter(t -> t.getStatus() == TaskStatus.SUCCEEDED
                                || t.getStatus() == TaskStatus.WAITING_APPROVAL)
                        .count(),
                Math.max(ctx.tasks().stream()
                        .filter(t -> t.getStatus() == TaskStatus.WAITING_APPROVAL
                                || t.getStatus() == TaskStatus.BLOCKED)
                        .count(), 1));
        long ciTotal = ctx.tasks().stream()
                .filter(t -> DashboardAggregationService.resolveStage(t)
                        == ai.nova.platform.dashboard.entity.PipelineStageCode.CI)
                .count();
        long ciPassed = ctx.tasks().stream()
                .filter(t -> DashboardAggregationService.resolveStage(t)
                        == ai.nova.platform.dashboard.entity.PipelineStageCode.CI)
                .filter(t -> t.getStatus() == TaskStatus.SUCCEEDED)
                .count();
        double ciPassRate = rate(ciPassed, ciTotal);
        double rollbackReadyRate = rate(
                ctx.rollbacks().stream().filter(r -> r.getStatus() == RollbackStatus.READY).count(),
                ctx.rollbacks().size());

        return new DashboardKpis(
                releaseSuccess,
                deploymentSuccess,
                pipelineSuccess,
                approvalSla,
                ciPassRate,
                rollbackReadyRate,
                avgReleaseDuration(ctx.releases()),
                avgExecutionDuration(ctx.executions()),
                avgTaskDuration(ctx.tasks()),
                0L,
                0L,
                avgRollbackDuration(ctx.rollbacks()));
    }

    private static boolean isActiveRun(RunStatus status) {
        return status == RunStatus.RUNNING || status == RunStatus.WAITING || status == RunStatus.READY;
    }

    private static double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (numerator * 100.0) / denominator;
    }

    private static long avgReleaseDuration(List<ai.nova.platform.release.entity.ReleaseOperationEntity> releases) {
        return average(releases.stream()
                .filter(r -> r.getPreparedAt() != null && r.getPublishedAt() != null)
                .mapToLong(r -> Duration.between(r.getPreparedAt(), r.getPublishedAt()).toMillis())
                .toArray());
    }

    private static long avgExecutionDuration(
            List<ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity> executions) {
        return average(executions.stream()
                .filter(e -> e.getDurationMs() != null)
                .mapToLong(ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity::getDurationMs)
                .toArray());
    }

    private static long avgTaskDuration(List<AgentOrchestrationTask> tasks) {
        return average(tasks.stream()
                .filter(t -> t.getStartedAt() != null && t.getCompletedAt() != null)
                .mapToLong(t -> Duration.between(t.getStartedAt(), t.getCompletedAt()).toMillis())
                .toArray());
    }

    private static long avgRollbackDuration(List<ai.nova.platform.rollback.entity.RollbackOperationEntity> rollbacks) {
        return average(rollbacks.stream()
                .filter(r -> r.getCreatedAt() != null && r.getValidatedAt() != null)
                .mapToLong(r -> Duration.between(r.getCreatedAt(), r.getValidatedAt()).toMillis())
                .toArray());
    }

    private static long average(long[] values) {
        if (values.length == 0) {
            return 0L;
        }
        long total = 0;
        for (long value : values) {
            total += value;
        }
        return total / values.length;
    }

    record DashboardCounts(
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
            long rollbackReadyCount) {}
}
