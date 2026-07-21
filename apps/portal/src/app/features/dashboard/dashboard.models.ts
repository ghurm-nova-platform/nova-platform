export type PipelineStageCode =
  | 'PLANNER'
  | 'CODING'
  | 'REVIEW'
  | 'TESTING'
  | 'PATCH'
  | 'GIT'
  | 'PULL_REQUEST'
  | 'CI'
  | 'REPAIR'
  | 'APPROVAL_GATE'
  | 'MERGE'
  | 'RELEASE'
  | 'DEPLOYMENT'
  | 'ROLLBACK';

export interface DashboardMeta {
  organizationId: string;
  projectId?: string | null;
  generatedAt: string;
  cacheExpiresAt: string;
  refreshRateSeconds: number;
  fromCache: boolean;
}

export interface DashboardKpis {
  releaseSuccessRate: number;
  deploymentSuccessRate: number;
  pipelineSuccessRate: number;
  approvalSlaComplianceRate: number;
  ciPassRate: number;
  rollbackReadinessRate: number;
  avgReleaseDurationMs: number;
  avgDeploymentDurationMs: number;
  avgPipelineStageDurationMs: number;
  avgApprovalWaitMs: number;
  avgCiDurationMs: number;
  avgRollbackPlanDurationMs: number;
}

export interface OverviewSection {
  projectCount: number;
  agentCount: number;
  activeRunCount: number;
  totalRunCount: number;
  releaseCount: number;
  deploymentCount: number;
  executionCount: number;
  environmentCount: number;
  auditEventCount: number;
  pendingApprovalCount: number;
  failedCiCount: number;
  rollbackReadyCount: number;
  kpis: DashboardKpis;
}

export interface PipelineStageSnapshot {
  stage: PipelineStageCode;
  label: string;
  current: number;
  waiting: number;
  failed: number;
  success: number;
  avgDurationMs: number;
}

export interface PipelineSection {
  stages: PipelineStageSnapshot[];
  totalActiveTasks: number;
}

export interface DeploymentExecutionSnapshot {
  id: string;
  projectId: string;
  environmentId: string;
  provider: string;
  environmentCode?: string | null;
  status: string;
  currentStage?: string | null;
  currentStep?: string | null;
  durationMs?: number | null;
  progressPercent: number;
  verifyStatus: string;
  startedAt?: string | null;
}

export interface DeploymentsSection {
  running: DeploymentExecutionSnapshot[];
  totalRunning: number;
  totalCompleted: number;
  totalFailed: number;
}

export interface ReleaseSnapshot {
  id: string;
  projectId: string;
  releaseName: string;
  semanticVersion: string;
  status: string;
  createdAt: string;
  publishedAt?: string | null;
}

export interface ReleasesSection {
  published: number;
  ready: number;
  blocked: number;
  pendingApproval: number;
  policyFailures: number;
  rollbackReady: number;
  recent: ReleaseSnapshot[];
}

export interface EnvironmentBucketSnapshot {
  bucket: string;
  environmentCount: number;
  healthy: number;
  degraded: number;
  unavailable: number;
  runningExecutions: number;
  recentDeployments: number;
}

export interface EnvironmentItemSnapshot {
  id: string;
  projectId: string;
  code: string;
  name: string;
  environmentType: string;
  status: string;
  health: string;
  runningExecutions: number;
  recentDeployments: number;
  labels: Record<string, string>;
}

export interface EnvironmentsSection {
  buckets: EnvironmentBucketSnapshot[];
  environments: EnvironmentItemSnapshot[];
}

export interface AuditEventSummary {
  id: string;
  entityType: string;
  action: string;
  result: string;
  severity: string;
  source: string;
  username?: string | null;
  createdAt: string;
}

export interface AuditSection {
  events: AuditEventSummary[];
  total: number;
}

export interface ApprovalQueueItem {
  taskId: string;
  runId: string;
  projectId: string;
  displayName: string;
  status: string;
  slaRemainingMs?: number | null;
  expired: boolean;
  blocked: boolean;
  waitingSince?: string | null;
}

export interface ApprovalsSection {
  waiting: number;
  expired: number;
  blocked: number;
  slaBreaches: number;
  queue: ApprovalQueueItem[];
}

export interface CiPipelineSnapshot {
  taskId: string;
  projectId: string;
  provider: string;
  repository: string;
  branch: string;
  overallStatus: string;
  durationMs?: number | null;
  completedAt?: string | null;
  failed: boolean;
}

export interface CiSection {
  recentPipelines: CiPipelineSnapshot[];
  failedBuilds: number;
  repairRequests: number;
  queueDepth: number;
  avgDurationMs: number;
}

export interface RollbackSnapshot {
  id: string;
  projectId: string;
  currentVersion: string;
  targetVersion: string;
  environmentCode: string;
  status: string;
  durationMs?: number | null;
  createdAt: string;
}

export interface RollbacksSection {
  ready: number;
  executed: number;
  failed: number;
  coveragePercent: number;
  avgDurationMs: number;
  recent: RollbackSnapshot[];
}

export interface CostProviderUsage {
  provider: string;
  estimatedCost: number;
  operationCount: number;
}

export interface CostSection {
  estimatedTotalCost: number;
  providerUsage: CostProviderUsage[];
  futureLlmCostEstimate: number;
  note: string;
}

export interface DashboardSnapshot {
  meta: DashboardMeta;
  overview: OverviewSection;
  pipeline: PipelineSection;
  deployments: DeploymentsSection;
  releases: ReleasesSection;
  environments: EnvironmentsSection;
  audit: AuditSection;
  approvals: ApprovalsSection;
  ci: CiSection;
  rollbacks: RollbacksSection;
  cost: CostSection;
}

export interface DashboardConfigResponse {
  enabled: boolean;
  refreshRateSeconds: number;
  cacheTtlSeconds: number;
}

export type DashboardExportFormat = 'csv' | 'xlsx' | 'pdf';
export type DashboardExportSection =
  | 'overview'
  | 'pipeline'
  | 'deployments'
  | 'releases'
  | 'environments'
  | 'audit'
  | 'approvals'
  | 'ci'
  | 'rollbacks'
  | 'cost';
