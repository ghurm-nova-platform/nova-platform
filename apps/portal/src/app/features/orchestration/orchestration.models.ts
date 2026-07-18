export type RunStatus =
  | 'DRAFT'
  | 'READY'
  | 'RUNNING'
  | 'WAITING'
  | 'SUCCEEDED'
  | 'PARTIALLY_SUCCEEDED'
  | 'FAILED'
  | 'CANCEL_REQUESTED'
  | 'CANCELLED'
  | 'TIMED_OUT'
  | 'ARCHIVED';

export type ExecutionMode = 'SEQUENTIAL' | 'DEPENDENCY_GRAPH';

export type FailurePolicy = 'FAIL_FAST' | 'CONTINUE_INDEPENDENT' | 'BEST_EFFORT';

export type TaskType = 'AGENT_TURN' | 'HUMAN_APPROVAL' | 'TRANSFORM' | 'AGGREGATION';

export type TaskStatus =
  | 'DRAFT'
  | 'BLOCKED'
  | 'READY'
  | 'CLAIMED'
  | 'RUNNING'
  | 'RETRY_WAIT'
  | 'WAITING_APPROVAL'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'SKIPPED'
  | 'CANCEL_REQUESTED'
  | 'CANCELLED'
  | 'TIMED_OUT';

export type DependencyType = 'SUCCESS' | 'COMPLETION';

export type OrchestrationEventType =
  | 'RUN_CREATED'
  | 'RUN_READY'
  | 'RUN_STARTED'
  | 'RUN_WAITING'
  | 'RUN_SUCCEEDED'
  | 'RUN_PARTIALLY_SUCCEEDED'
  | 'RUN_FAILED'
  | 'RUN_CANCEL_REQUESTED'
  | 'RUN_CANCELLED'
  | 'RUN_TIMED_OUT'
  | 'RUN_ARCHIVED'
  | 'TASK_CREATED'
  | 'TASK_UPDATED'
  | 'TASK_BLOCKED'
  | 'TASK_READY'
  | 'TASK_CLAIMED'
  | 'TASK_STARTED'
  | 'TASK_RETRY_SCHEDULED'
  | 'TASK_SUCCEEDED'
  | 'TASK_FAILED'
  | 'TASK_SKIPPED'
  | 'TASK_CANCEL_REQUESTED'
  | 'TASK_CANCELLED'
  | 'TASK_TIMED_OUT'
  | 'TASK_STALE_RESULT_IGNORED'
  | 'DEPENDENCY_ADDED'
  | 'DEPENDENCY_REMOVED';

export type AttemptStatus = 'STARTED' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'TIMED_OUT' | 'STALE';

export const RUN_STATUSES: RunStatus[] = [
  'DRAFT',
  'READY',
  'RUNNING',
  'WAITING',
  'SUCCEEDED',
  'PARTIALLY_SUCCEEDED',
  'FAILED',
  'CANCEL_REQUESTED',
  'CANCELLED',
  'TIMED_OUT',
  'ARCHIVED',
];

export const EXECUTION_MODES: ExecutionMode[] = ['SEQUENTIAL', 'DEPENDENCY_GRAPH'];

export const FAILURE_POLICIES: FailurePolicy[] = ['FAIL_FAST', 'CONTINUE_INDEPENDENT', 'BEST_EFFORT'];

export const TASK_TYPES: TaskType[] = ['AGENT_TURN', 'HUMAN_APPROVAL', 'TRANSFORM', 'AGGREGATION'];

export const TASK_STATUSES: TaskStatus[] = [
  'DRAFT',
  'BLOCKED',
  'READY',
  'CLAIMED',
  'RUNNING',
  'RETRY_WAIT',
  'WAITING_APPROVAL',
  'SUCCEEDED',
  'FAILED',
  'SKIPPED',
  'CANCEL_REQUESTED',
  'CANCELLED',
  'TIMED_OUT',
];

export const DEPENDENCY_TYPES: DependencyType[] = ['SUCCESS', 'COMPLETION'];

export const ACTIVE_RUN_STATUSES: RunStatus[] = ['RUNNING', 'WAITING', 'CANCEL_REQUESTED'];

export interface OrchestrationRun {
  id: string;
  organizationId: string;
  projectId: string;
  initiatedByAgentId: string | null;
  rootExecutionId: string | null;
  name: string;
  objective: string;
  status: RunStatus;
  executionMode: ExecutionMode;
  failurePolicy: FailurePolicy;
  maxParallelTasks: number;
  maximumDurationMs: number;
  startedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  deadlineAt: string | null;
  cancellationReason: string | null;
  failureCode: string | null;
  failureMessage: string | null;
  inputJson: string | null;
  outputJson: string | null;
  metadataJson: string | null;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  taskStatusCounts: Record<string, number>;
  runningTaskCount: number;
  completedPercentage: number;
}

export interface OrchestrationTask {
  id: string;
  organizationId: string;
  projectId: string;
  runId: string;
  taskKey: string;
  displayName: string;
  description: string | null;
  taskType: TaskType;
  status: TaskStatus;
  assignedAgentId: string | null;
  modelReference: string | null;
  requiredCapabilitiesJson: string | null;
  inputJson: string | null;
  outputJson: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  attemptCount: number;
  maxAttempts: number;
  retryBackoffMs: number;
  nextAttemptAt: string | null;
  priority: number;
  timeoutSeconds: number;
  sequenceOrder: number | null;
  idempotencyKey: string | null;
  startedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  claimedAt: string | null;
  claimedBy: string | null;
  claimExpiresAt: string | null;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface OrchestrationDependency {
  runId: string;
  predecessorTaskId: string;
  successorTaskId: string;
  dependencyType: DependencyType;
  createdAt: string;
}

export interface OrchestrationGraph {
  runId: string;
  nodes: OrchestrationTask[];
  edges: OrchestrationDependency[];
}

export interface OrchestrationEvent {
  id: string;
  runId: string;
  taskId: string | null;
  eventType: OrchestrationEventType;
  eventSequence: number;
  payloadJson: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface OrchestrationAttempt {
  id: string;
  runId: string;
  taskId: string;
  attemptNumber: number;
  status: AttemptStatus;
  executionId: string | null;
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number | null;
  inputSnapshotJson: string | null;
  outputSnapshotJson: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  retryable: boolean;
  workerId: string | null;
  createdAt: string;
}

export interface CreateOrchestrationRunRequest {
  projectId: string;
  name: string;
  objective: string;
  executionMode: ExecutionMode;
  failurePolicy: FailurePolicy;
  maxParallelTasks?: number | null;
  maximumDurationMs?: number | null;
  initiatedByAgentId?: string | null;
  inputJson?: string | null;
  metadataJson?: string | null;
}

export interface UpdateOrchestrationRunRequest {
  version: number;
  name: string;
  objective: string;
  executionMode: ExecutionMode;
  failurePolicy: FailurePolicy;
  maxParallelTasks?: number | null;
  maximumDurationMs?: number | null;
  initiatedByAgentId?: string | null;
  inputJson?: string | null;
  metadataJson?: string | null;
}

export interface CancelOrchestrationRunRequest {
  reason?: string | null;
}

export interface CreateOrchestrationTaskRequest {
  taskKey: string;
  displayName: string;
  description?: string | null;
  taskType: TaskType;
  assignedAgentId?: string | null;
  modelReference?: string | null;
  requiredCapabilitiesJson?: string | null;
  inputJson?: string | null;
  maxAttempts?: number | null;
  retryBackoffMs?: number | null;
  priority?: number | null;
  timeoutSeconds?: number | null;
  sequenceOrder?: number | null;
  idempotencyKey?: string | null;
}

export interface UpdateOrchestrationTaskRequest {
  version: number;
  taskKey: string;
  displayName: string;
  description?: string | null;
  taskType: TaskType;
  assignedAgentId?: string | null;
  modelReference?: string | null;
  requiredCapabilitiesJson?: string | null;
  inputJson?: string | null;
  maxAttempts?: number | null;
  retryBackoffMs?: number | null;
  priority?: number | null;
  timeoutSeconds?: number | null;
  sequenceOrder?: number | null;
  idempotencyKey?: string | null;
}

export interface CreateOrchestrationDependencyRequest {
  predecessorTaskId: string;
  successorTaskId: string;
  dependencyType: DependencyType;
}

export interface DeleteOrchestrationDependencyRequest {
  predecessorTaskId: string;
  successorTaskId: string;
}

export interface OrchestrationRunListParams {
  projectId?: string;
  status?: RunStatus;
  executionMode?: ExecutionMode;
  createdBy?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface OrchestrationTaskListParams {
  status?: TaskStatus;
  taskType?: TaskType;
  page?: number;
  size?: number;
  sort?: string;
}

export interface OrchestrationEventListParams {
  taskId?: string;
  page?: number;
  size?: number;
  sort?: string;
}
