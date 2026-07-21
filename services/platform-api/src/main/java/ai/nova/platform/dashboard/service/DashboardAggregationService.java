package ai.nova.platform.dashboard.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.entity.ApprovalOperationStatus;
import ai.nova.platform.approval.repository.ApprovalGateOperationRepository;
import ai.nova.platform.audit.dto.AuditDtos.AuditSearchRequest;
import ai.nova.platform.audit.service.AuditSearchService;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.repository.CiObservationOperationRepository;
import ai.nova.platform.dashboard.dto.DashboardDtos.ApprovalQueueItem;
import ai.nova.platform.dashboard.dto.DashboardDtos.ApprovalsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.AuditSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CiPipelineSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.CiSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CostProviderUsage;
import ai.nova.platform.dashboard.dto.DashboardDtos.CostSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.DeploymentExecutionSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.DeploymentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentBucketSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentItemSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.OverviewSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.PipelineSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.PipelineStageSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleaseSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleasesSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.RollbackSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.RollbacksSection;
import ai.nova.platform.dashboard.entity.PipelineStageCode;
import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.deployment.repository.DeploymentOperationRepository;
import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionRepository;
import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.planner.repository.PlannerTemplateRepository;
import ai.nova.platform.policy.repository.PolicyEvaluationRepository;
import ai.nova.platform.project.Project;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.repository.ReleaseOperationRepository;
import ai.nova.platform.rollback.entity.RollbackOperationEntity;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.repository.RollbackOperationRepository;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class DashboardAggregationService {

    private static final int RECENT_LIMIT = 200;
    private static final Set<ExecutionStatus> ACTIVE_EXECUTION_STATUSES = Set.of(
            ExecutionStatus.READY,
            ExecutionStatus.QUEUED,
            ExecutionStatus.STARTING,
            ExecutionStatus.DEPLOYING,
            ExecutionStatus.VERIFYING);
    private static final Set<RunStatus> ACTIVE_RUN_STATUSES = Set.of(
            RunStatus.RUNNING, RunStatus.WAITING, RunStatus.READY);
    private static final Set<TaskStatus> CURRENT_TASK_STATUSES = Set.of(
            TaskStatus.CLAIMED, TaskStatus.RUNNING);
    private static final Set<TaskStatus> WAITING_TASK_STATUSES = Set.of(
            TaskStatus.READY,
            TaskStatus.BLOCKED,
            TaskStatus.RETRY_WAIT,
            TaskStatus.WAITING_APPROVAL);
    private static final Set<TaskStatus> FAILED_TASK_STATUSES = Set.of(TaskStatus.FAILED, TaskStatus.TIMED_OUT);
    private static final Set<TaskStatus> SUCCESS_TASK_STATUSES = Set.of(TaskStatus.SUCCEEDED);

    private final ProjectRepository projectRepository;
    private final AgentRepository agentRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final ReleaseOperationRepository releaseOperationRepository;
    private final DeploymentOperationRepository deploymentOperationRepository;
    private final DeploymentExecutionRepository deploymentExecutionRepository;
    private final DeploymentEnvironmentRepository deploymentEnvironmentRepository;
    private final AuditSearchService auditSearchService;
    private final ApprovalGateOperationRepository approvalGateOperationRepository;
    private final CiObservationOperationRepository ciObservationOperationRepository;
    private final RollbackOperationRepository rollbackOperationRepository;
    private final PolicyEvaluationRepository policyEvaluationRepository;
    private final PlannerTemplateRepository plannerTemplateRepository;

    public DashboardAggregationService(
            ProjectRepository projectRepository,
            AgentRepository agentRepository,
            AgentOrchestrationRunRepository runRepository,
            AgentOrchestrationTaskRepository taskRepository,
            ReleaseOperationRepository releaseOperationRepository,
            DeploymentOperationRepository deploymentOperationRepository,
            DeploymentExecutionRepository deploymentExecutionRepository,
            DeploymentEnvironmentRepository deploymentEnvironmentRepository,
            AuditSearchService auditSearchService,
            ApprovalGateOperationRepository approvalGateOperationRepository,
            CiObservationOperationRepository ciObservationOperationRepository,
            RollbackOperationRepository rollbackOperationRepository,
            PolicyEvaluationRepository policyEvaluationRepository,
            PlannerTemplateRepository plannerTemplateRepository) {
        this.projectRepository = projectRepository;
        this.agentRepository = agentRepository;
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.releaseOperationRepository = releaseOperationRepository;
        this.deploymentOperationRepository = deploymentOperationRepository;
        this.deploymentExecutionRepository = deploymentExecutionRepository;
        this.deploymentEnvironmentRepository = deploymentEnvironmentRepository;
        this.auditSearchService = auditSearchService;
        this.approvalGateOperationRepository = approvalGateOperationRepository;
        this.ciObservationOperationRepository = ciObservationOperationRepository;
        this.rollbackOperationRepository = rollbackOperationRepository;
        this.policyEvaluationRepository = policyEvaluationRepository;
        this.plannerTemplateRepository = plannerTemplateRepository;
    }

    @Transactional(readOnly = true)
    public OverviewSection aggregateOverview(AuthenticatedUser user, UUID projectId) {
        AggregationContext ctx = loadContext(user, projectId);
        DashboardMetricsService.DashboardCounts counts = DashboardMetricsService.countOverview(ctx);
        return new OverviewSection(
                counts.projectCount(),
                counts.agentCount(),
                counts.activeRunCount(),
                counts.totalRunCount(),
                counts.releaseCount(),
                counts.deploymentCount(),
                counts.executionCount(),
                counts.environmentCount(),
                counts.auditEventCount(),
                counts.pendingApprovalCount(),
                counts.failedCiCount(),
                counts.rollbackReadyCount(),
                DashboardMetricsService.computeKpis(ctx));
    }

    @Transactional(readOnly = true)
    public PipelineSection aggregatePipeline(AuthenticatedUser user, UUID projectId) {
        AggregationContext ctx = loadContext(user, projectId);
        Map<PipelineStageCode, StageAccumulator> accumulators = new EnumMap<>(PipelineStageCode.class);
        for (PipelineStageCode stage : PipelineStageCode.values()) {
            accumulators.put(stage, new StageAccumulator(stage));
        }

        for (AgentOrchestrationTask task : ctx.tasks()) {
            PipelineStageCode stage = resolveStage(task);
            StageAccumulator acc = accumulators.get(stage);
            if (CURRENT_TASK_STATUSES.contains(task.getStatus())) {
                acc.current++;
            } else if (WAITING_TASK_STATUSES.contains(task.getStatus())) {
                acc.waiting++;
            } else if (FAILED_TASK_STATUSES.contains(task.getStatus())) {
                acc.failed++;
            } else if (SUCCESS_TASK_STATUSES.contains(task.getStatus())) {
                acc.success++;
            }
            Long duration = taskDurationMs(task);
            if (duration != null) {
                acc.durationTotal += duration;
                acc.durationCount++;
            }
        }

        long plannerTemplates = projectId != null
                ? plannerTemplateRepository.findEnabledForProject(user.getOrganizationId(), projectId).size()
                : ctx.projects().stream()
                        .mapToInt(p -> plannerTemplateRepository
                                .findEnabledForProject(user.getOrganizationId(), p.getId())
                                .size())
                        .sum();
        accumulators.get(PipelineStageCode.PLANNER).waiting += plannerTemplates;

        List<PipelineStageSnapshot> stages = accumulators.values().stream()
                .map(StageAccumulator::toSnapshot)
                .toList();
        long totalActive = ctx.tasks().stream()
                .filter(t -> CURRENT_TASK_STATUSES.contains(t.getStatus())
                        || WAITING_TASK_STATUSES.contains(t.getStatus()))
                .count();
        return new PipelineSection(stages, totalActive);
    }

    @Transactional(readOnly = true)
    public DeploymentsSection aggregateDeployments(AuthenticatedUser user, UUID projectId) {
        List<DeploymentExecutionEntity> executions = filterExecutions(user.getOrganizationId(), projectId);
        List<DeploymentExecutionSnapshot> running = executions.stream()
                .filter(e -> ACTIVE_EXECUTION_STATUSES.contains(e.getStatus()))
                .limit(50)
                .map(this::toDeploymentSnapshot)
                .toList();
        long completed = executions.stream().filter(e -> e.getStatus() == ExecutionStatus.COMPLETED).count();
        long failed = executions.stream().filter(e -> e.getStatus() == ExecutionStatus.FAILED).count();
        return new DeploymentsSection(running, running.size(), completed, failed);
    }

    @Transactional(readOnly = true)
    public ReleasesSection aggregateReleases(AuthenticatedUser user, UUID projectId) {
        List<ReleaseOperationEntity> releases = filterReleases(user.getOrganizationId(), projectId);
        long published = releases.stream().filter(r -> r.getStatus() == ReleaseStatus.PUBLISHED).count();
        long ready = releases.stream().filter(r -> r.getStatus() == ReleaseStatus.READY).count();
        long blocked = releases.stream()
                .filter(r -> r.getStatus() == ReleaseStatus.FAILED || r.getStatus() == ReleaseStatus.PREPARING)
                .count();
        long pendingApproval = releases.stream().filter(r -> r.getStatus() == ReleaseStatus.READY).count();
        long policyFailures = releases.stream()
                .filter(r -> r.getErrorCode() != null && r.getErrorCode().toUpperCase(Locale.ROOT).contains("POLICY"))
                .count();
        long rollbackReady = filterRollbacks(user.getOrganizationId(), projectId).stream()
                .filter(r -> r.getStatus() == RollbackStatus.READY || r.getStatus() == RollbackStatus.VALIDATING)
                .count();
        List<ReleaseSnapshot> recent = releases.stream().limit(25).map(this::toReleaseSnapshot).toList();
        return new ReleasesSection(
                published, ready, blocked, pendingApproval, policyFailures, rollbackReady, recent);
    }

    @Transactional(readOnly = true)
    public EnvironmentsSection aggregateEnvironments(AuthenticatedUser user, UUID projectId) {
        List<DeploymentEnvironmentEntity> environments = loadEnvironments(user.getOrganizationId(), projectId);
        List<EnvironmentItemSnapshot> items = new ArrayList<>();
        Map<String, EnvironmentBucketSnapshotBuilder> buckets = new HashMap<>();
        for (String bucket : List.of("production", "staging", "qa", "dev", "other")) {
            buckets.put(bucket, new EnvironmentBucketSnapshotBuilder(bucket));
        }

        for (DeploymentEnvironmentEntity env : environments) {
            String bucket = bucketFor(env);
            long runningExecs = deploymentExecutionRepository.countByOrganizationIdAndEnvironmentIdAndStatusIn(
                    user.getOrganizationId(), env.getId(), ACTIVE_EXECUTION_STATUSES);
            long recentDeployments = filterExecutions(user.getOrganizationId(), projectId).stream()
                    .filter(e -> env.getId().equals(e.getEnvironmentId()))
                    .count();
            String health = resolveHealth(env, runningExecs);
            items.add(new EnvironmentItemSnapshot(
                    env.getId(),
                    env.getProjectId(),
                    env.getCode(),
                    env.getName(),
                    env.getEnvironmentType() != null ? env.getEnvironmentType().name() : "CUSTOM",
                    env.getStatus() != null ? env.getStatus().name() : EnvironmentStatus.ACTIVE.name(),
                    health,
                    runningExecs,
                    recentDeployments,
                    parseLabels(env.getTagsJson())));

            EnvironmentBucketSnapshotBuilder builder = buckets.get(bucket);
            builder.environmentCount++;
            switch (health) {
                case "HEALTHY" -> builder.healthy++;
                case "DEGRADED" -> builder.degraded++;
                default -> builder.unavailable++;
            }
            builder.runningExecutions += runningExecs;
            builder.recentDeployments += recentDeployments;
        }

        return new EnvironmentsSection(
                buckets.values().stream().map(EnvironmentBucketSnapshotBuilder::build).toList(), items);
    }

    @Transactional(readOnly = true)
    public AuditSection aggregateAudit(AuthenticatedUser user, UUID projectId, AuditSearchRequest filters) {
        var response = auditSearchService.search(
                filters != null
                        ? filters
                        : new AuditSearchRequest(
                                null, null, projectId, null, null, null, null, null, null, null, null, 0, 25),
                user);
        return new AuditSection(response.events(), response.total());
    }

    @Transactional(readOnly = true)
    public ApprovalsSection aggregateApprovals(AuthenticatedUser user, UUID projectId) {
        AggregationContext ctx = loadContext(user, projectId);
        List<ApprovalQueueItem> queue = new ArrayList<>();
        long waiting = 0;
        long expired = 0;
        long blocked = 0;
        long slaBreaches = 0;
        Instant now = Instant.now();

        for (AgentOrchestrationTask task : ctx.tasks()) {
            if (task.getStatus() != TaskStatus.WAITING_APPROVAL && task.getStatus() != TaskStatus.BLOCKED) {
                continue;
            }
            var latestApproval = approvalGateOperationRepository
                    .findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(task.getId(), user.getOrganizationId());
            boolean isBlocked = task.getStatus() == TaskStatus.BLOCKED
                    || latestApproval.map(op -> op.getStatus() == ApprovalOperationStatus.FAILED).orElse(false);
            boolean isExpired = latestApproval
                    .map(op -> op.getCompletedAt() != null
                            && op.getDecision() == ApprovalDecisionValue.REJECTED)
                    .orElse(false);
            Instant waitingSince = task.getStartedAt() != null ? task.getStartedAt() : task.getCreatedAt();
            Long slaRemaining = waitingSince != null ? Duration.ofHours(24).toMillis()
                    - Duration.between(waitingSince, now).toMillis() : null;
            if (slaRemaining != null && slaRemaining < 0) {
                slaBreaches++;
            }
            if (isBlocked) {
                blocked++;
            } else if (isExpired) {
                expired++;
            } else {
                waiting++;
            }
            queue.add(new ApprovalQueueItem(
                    task.getId(),
                    task.getRunId(),
                    task.getProjectId(),
                    task.getDisplayName(),
                    task.getStatus().name(),
                    slaRemaining,
                    isExpired,
                    isBlocked,
                    waitingSince));
        }
        return new ApprovalsSection(waiting, expired, blocked, slaBreaches, queue.stream().limit(50).toList());
    }

    @Transactional(readOnly = true)
    public CiSection aggregateCi(AuthenticatedUser user, UUID projectId) {
        AggregationContext ctx = loadContext(user, projectId);
        List<CiPipelineSnapshot> pipelines = new ArrayList<>();
        long failedBuilds = 0;
        long repairRequests = 0;
        long queueDepth = 0;
        long durationTotal = 0;
        long durationCount = 0;

        for (AgentOrchestrationTask task : ctx.tasks()) {
            if (resolveStage(task) != PipelineStageCode.CI && task.getStatus() != TaskStatus.RUNNING) {
                continue;
            }
            var ciOps = ciObservationOperationRepository.findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
                    task.getId(), user.getOrganizationId());
            if (ciOps.isEmpty()) {
                if (task.getStatus() == TaskStatus.READY || task.getStatus() == TaskStatus.CLAIMED) {
                    queueDepth++;
                }
                continue;
            }
            var latest = ciOps.getFirst();
            boolean failed = latest.getOverallStatus() == CiOverallStatus.FAILED
                    || latest.getOverallStatus() == CiOverallStatus.TIMED_OUT
                    || latest.getOverallStatus() == CiOverallStatus.CANCELLED;
            if (failed) {
                failedBuilds++;
            }
            if (latest.getRetryRecommendation() != null && !latest.getRetryRecommendation().isBlank()) {
                repairRequests++;
            }
            Long duration = ciDurationMs(latest.getStartedAt(), latest.getCompletedAt());
            if (duration != null) {
                durationTotal += duration;
                durationCount++;
            }
            pipelines.add(new CiPipelineSnapshot(
                    task.getId(),
                    task.getProjectId(),
                    latest.getProvider(),
                    joinRepo(latest.getRepositoryOwner(), latest.getRepositoryName()),
                    latest.getSourceBranch(),
                    latest.getOverallStatus().name(),
                    duration,
                    latest.getCompletedAt(),
                    failed));
        }

        long avgDuration = durationCount == 0 ? 0L : durationTotal / durationCount;
        return new CiSection(pipelines.stream().limit(50).toList(), failedBuilds, repairRequests, queueDepth, avgDuration);
    }

    @Transactional(readOnly = true)
    public RollbacksSection aggregateRollbacks(AuthenticatedUser user, UUID projectId) {
        List<RollbackOperationEntity> rollbacks = filterRollbacks(user.getOrganizationId(), projectId);
        long ready = rollbacks.stream()
                .filter(r -> r.getStatus() == RollbackStatus.READY || r.getStatus() == RollbackStatus.VALIDATING)
                .count();
        long executed = rollbacks.stream().filter(r -> r.getStatus() == RollbackStatus.SUCCEEDED).count();
        long failed = rollbacks.stream().filter(r -> r.getStatus() == RollbackStatus.FAILED).count();
        long durationTotal = 0;
        long durationCount = 0;
        for (RollbackOperationEntity rollback : rollbacks) {
            Long duration = ciDurationMs(rollback.getCreatedAt(), rollback.getValidatedAt());
            if (duration != null) {
                durationTotal += duration;
                durationCount++;
            }
        }
        long avgDuration = durationCount == 0 ? 0L : durationTotal / durationCount;
        double coverage = rollbacks.isEmpty() ? 0.0 : (ready * 100.0) / rollbacks.size();
        List<RollbackSnapshot> recent = rollbacks.stream().limit(25).map(this::toRollbackSnapshot).toList();
        return new RollbacksSection(ready, executed, failed, coverage, avgDuration, recent);
    }

    @Transactional(readOnly = true)
    public CostSection aggregateCost(AuthenticatedUser user, UUID projectId) {
        List<DeploymentExecutionEntity> executions = filterExecutions(user.getOrganizationId(), projectId);
        Map<String, Long> providerCounts = executions.stream()
                .collect(Collectors.groupingBy(e -> e.getProvider().name(), Collectors.counting()));
        List<CostProviderUsage> usage = providerCounts.entrySet().stream()
                .map(entry -> new CostProviderUsage(entry.getKey(), entry.getValue() * 0.0, entry.getValue()))
                .toList();
        return new CostSection(
                0.0,
                usage,
                0.0,
                "Cost estimates are placeholders until billing integration is available.");
    }

    AggregationContext loadContext(AuthenticatedUser user, UUID projectId) {
        UUID orgId = user.getOrganizationId();
        List<Project> projects = loadProjects(orgId, projectId);
        long agentCount = 0;
        for (Project project : projects) {
            agentCount += agentRepository
                    .search(orgId, project.getId(), null, null, PageRequest.of(0, 1))
                    .getTotalElements();
        }
        List<AgentOrchestrationRun> runs = loadRuns(orgId, projectId);
        List<AgentOrchestrationTask> tasks = new ArrayList<>();
        for (AgentOrchestrationRun run : runs) {
            tasks.addAll(taskRepository.findByRunIdAndOrganizationId(run.getId(), orgId));
        }
        List<ReleaseOperationEntity> releases = filterReleases(orgId, projectId);
        List<DeploymentExecutionEntity> executions = filterExecutions(orgId, projectId);
        var audit = auditSearchService.listRecent(0, 1, user);
        return new AggregationContext(
                orgId,
                projectId,
                projects,
                agentCount,
                runs,
                tasks,
                releases,
                filterDeployments(orgId, projectId),
                executions,
                loadEnvironments(orgId, projectId),
                audit.total(),
                filterRollbacks(orgId, projectId),
                policyEvaluationRepository.count());
    }

    private List<Project> loadProjects(UUID orgId, UUID projectId) {
        if (projectId != null) {
            return projectRepository
                    .findByIdAndOrganizationId(projectId, orgId)
                    .map(List::of)
                    .orElse(List.of());
        }
        return projectRepository
                .searchByOrganization(orgId, null, PageRequest.of(0, RECENT_LIMIT))
                .getContent();
    }

    private List<AgentOrchestrationRun> loadRuns(UUID orgId, UUID projectId) {
        return runRepository
                .search(orgId, projectId, null, null, null, null, PageRequest.of(0, RECENT_LIMIT))
                .getContent();
    }

    private List<ReleaseOperationEntity> filterReleases(UUID orgId, UUID projectId) {
        List<ReleaseOperationEntity> releases = projectId != null
                ? releaseOperationRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(orgId, projectId)
                : releaseOperationRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        return releases.stream().limit(RECENT_LIMIT).toList();
    }

    private List<ai.nova.platform.deployment.entity.DeploymentOperationEntity> filterDeployments(
            UUID orgId, UUID projectId) {
        List<ai.nova.platform.deployment.entity.DeploymentOperationEntity> deployments = projectId != null
                ? deploymentOperationRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(orgId, projectId)
                : deploymentOperationRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        return deployments.stream().limit(RECENT_LIMIT).toList();
    }

    private List<DeploymentExecutionEntity> filterExecutions(UUID orgId, UUID projectId) {
        List<DeploymentExecutionEntity> executions = projectId != null
                ? deploymentExecutionRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(orgId, projectId)
                : deploymentExecutionRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        return executions.stream().limit(RECENT_LIMIT).toList();
    }

    private List<RollbackOperationEntity> filterRollbacks(UUID orgId, UUID projectId) {
        List<RollbackOperationEntity> rollbacks = projectId != null
                ? rollbackOperationRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(orgId, projectId)
                : rollbackOperationRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        return rollbacks.stream().limit(RECENT_LIMIT).toList();
    }

    private List<DeploymentEnvironmentEntity> loadEnvironments(UUID orgId, UUID projectId) {
        List<DeploymentEnvironmentEntity> environments = new ArrayList<>();
        if (projectId != null) {
            environments.addAll(
                    deploymentEnvironmentRepository.findByOrganizationIdAndProjectIdOrderBySortOrderAscCreatedAtDesc(
                            orgId, projectId));
        } else {
            for (Project project : loadProjects(orgId, null)) {
                environments.addAll(
                        deploymentEnvironmentRepository
                                .findByOrganizationIdAndProjectIdOrderBySortOrderAscCreatedAtDesc(
                                        orgId, project.getId()));
            }
        }
        return environments.stream().limit(RECENT_LIMIT).toList();
    }

    private DeploymentExecutionSnapshot toDeploymentSnapshot(DeploymentExecutionEntity entity) {
        int progress = progressPercent(entity.getStatus());
        String verifyStatus = entity.getStatus() == ExecutionStatus.VERIFYING ? "IN_PROGRESS" : "PENDING";
        if (entity.getStatus() == ExecutionStatus.COMPLETED) {
            verifyStatus = "PASSED";
        } else if (entity.getStatus() == ExecutionStatus.FAILED) {
            verifyStatus = "FAILED";
        }
        return new DeploymentExecutionSnapshot(
                entity.getId(),
                entity.getProjectId(),
                entity.getEnvironmentId(),
                entity.getProvider().name(),
                null,
                entity.getStatus().name(),
                entity.getCurrentStage(),
                entity.getCurrentStep(),
                entity.getDurationMs(),
                progress,
                verifyStatus,
                entity.getStartedAt());
    }

    private ReleaseSnapshot toReleaseSnapshot(ReleaseOperationEntity entity) {
        return new ReleaseSnapshot(
                entity.getId(),
                entity.getProjectId(),
                entity.getReleaseName(),
                entity.getSemanticVersion(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getPublishedAt());
    }

    private RollbackSnapshot toRollbackSnapshot(RollbackOperationEntity entity) {
        return new RollbackSnapshot(
                entity.getId(),
                entity.getProjectId(),
                entity.getCurrentVersion(),
                entity.getTargetVersion(),
                entity.getEnvironmentCode(),
                entity.getStatus().name(),
                ciDurationMs(entity.getCreatedAt(), entity.getValidatedAt()),
                entity.getCreatedAt());
    }

    public static PipelineStageCode resolveStage(AgentOrchestrationTask task) {
        String key = (task.getTaskKey() + " " + task.getDisplayName()).toLowerCase(Locale.ROOT);
        if (containsAny(key, "planner", "plan")) {
            return PipelineStageCode.PLANNER;
        }
        if (containsAny(key, "coding", "code", "artifact")) {
            return PipelineStageCode.CODING;
        }
        if (containsAny(key, "review")) {
            return PipelineStageCode.REVIEW;
        }
        if (containsAny(key, "testing", "test")) {
            return PipelineStageCode.TESTING;
        }
        if (containsAny(key, "patch", "diff")) {
            return PipelineStageCode.PATCH;
        }
        if (containsAny(key, "git", "branch", "commit")) {
            return PipelineStageCode.GIT;
        }
        if (containsAny(key, "pull", "pr", "request")) {
            return PipelineStageCode.PULL_REQUEST;
        }
        if (containsAny(key, "ci", "pipeline", "workflow")) {
            return PipelineStageCode.CI;
        }
        if (containsAny(key, "repair", "fix")) {
            return PipelineStageCode.REPAIR;
        }
        if (containsAny(key, "approval", "gate")) {
            return PipelineStageCode.APPROVAL_GATE;
        }
        if (containsAny(key, "merge")) {
            return PipelineStageCode.MERGE;
        }
        if (containsAny(key, "release")) {
            return PipelineStageCode.RELEASE;
        }
        if (containsAny(key, "deploy", "deployment", "execution")) {
            return PipelineStageCode.DEPLOYMENT;
        }
        if (containsAny(key, "rollback")) {
            return PipelineStageCode.ROLLBACK;
        }
        int order = task.getSequenceOrder() != null ? Math.max(task.getSequenceOrder(), 1) : 1;
        PipelineStageCode[] values = PipelineStageCode.values();
        return values[(order - 1) % values.length];
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static Long taskDurationMs(AgentOrchestrationTask task) {
        if (task.getStartedAt() == null || task.getCompletedAt() == null) {
            return null;
        }
        return Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis();
    }

    private static Long ciDurationMs(Instant startedAt, Instant completedAt) {
        if (startedAt == null || completedAt == null) {
            return null;
        }
        return Duration.between(startedAt, completedAt).toMillis();
    }

    private static int progressPercent(ExecutionStatus status) {
        return switch (status) {
            case READY, QUEUED -> 5;
            case STARTING -> 15;
            case DEPLOYING -> 55;
            case VERIFYING -> 85;
            case COMPLETED -> 100;
            case FAILED, CANCELLED -> 0;
        };
    }

    private static String bucketFor(DeploymentEnvironmentEntity env) {
        if (env.getEnvironmentType() == EnvironmentType.PRODUCTION) {
            return "production";
        }
        if (env.getEnvironmentType() == EnvironmentType.STAGING) {
            return "staging";
        }
        if (env.getEnvironmentType() == EnvironmentType.QA) {
            return "qa";
        }
        if (env.getEnvironmentType() == EnvironmentType.DEVELOPMENT
                || env.getEnvironmentType() == EnvironmentType.TESTING) {
            return "dev";
        }
        String code = env.getCode() != null ? env.getCode().toLowerCase(Locale.ROOT) : "";
        if (code.contains("prod")) {
            return "production";
        }
        if (code.contains("stag")) {
            return "staging";
        }
        if (code.contains("qa")) {
            return "qa";
        }
        if (code.contains("dev") || code.contains("test")) {
            return "dev";
        }
        return "other";
    }

    private static String resolveHealth(DeploymentEnvironmentEntity env, long runningExecutions) {
        if (env.getStatus() == EnvironmentStatus.DISABLED || env.getStatus() == EnvironmentStatus.ARCHIVED) {
            return "UNAVAILABLE";
        }
        if (!env.isActive()) {
            return "UNAVAILABLE";
        }
        if (runningExecutions > 0) {
            return "DEGRADED";
        }
        return "HEALTHY";
    }

    private static Map<String, String> parseLabels(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Map.of();
        }
        return Map.of("tags", tagsJson);
    }

    private static String joinRepo(String owner, String name) {
        if (owner == null && name == null) {
            return "";
        }
        if (owner == null) {
            return name;
        }
        if (name == null) {
            return owner;
        }
        return owner + "/" + name;
    }

    record AggregationContext(
            UUID organizationId,
            UUID projectId,
            List<Project> projects,
            long agentCount,
            List<AgentOrchestrationRun> runs,
            List<AgentOrchestrationTask> tasks,
            List<ReleaseOperationEntity> releases,
            List<ai.nova.platform.deployment.entity.DeploymentOperationEntity> deployments,
            List<DeploymentExecutionEntity> executions,
            List<DeploymentEnvironmentEntity> environments,
            long auditEventCount,
            List<RollbackOperationEntity> rollbacks,
            long policyEvaluationCount) {}

    private static final class StageAccumulator {
        private final PipelineStageCode stage;
        private long current;
        private long waiting;
        private long failed;
        private long success;
        private long durationTotal;
        private long durationCount;

        private StageAccumulator(PipelineStageCode stage) {
            this.stage = stage;
        }

        private PipelineStageSnapshot toSnapshot() {
            long avg = durationCount == 0 ? 0L : durationTotal / durationCount;
            return new PipelineStageSnapshot(
                    stage, stage.name(), current, waiting, failed, success, avg);
        }
    }

    private static final class EnvironmentBucketSnapshotBuilder {
        private final String bucket;
        private long environmentCount;
        private long healthy;
        private long degraded;
        private long unavailable;
        private long runningExecutions;
        private long recentDeployments;

        private EnvironmentBucketSnapshotBuilder(String bucket) {
            this.bucket = bucket;
        }

        private EnvironmentBucketSnapshot build() {
            return new EnvironmentBucketSnapshot(
                    bucket,
                    environmentCount,
                    healthy,
                    degraded,
                    unavailable,
                    runningExecutions,
                    recentDeployments);
        }
    }
}
