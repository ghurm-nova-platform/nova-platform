export type CiObservationStatus =
  | 'PENDING'
  | 'FETCHING'
  | 'PROCESSING'
  | 'SUCCEEDED'
  | 'FAILED';

export type CiOverallStatus =
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED'
  | 'TIMED_OUT'
  | 'IN_PROGRESS'
  | 'UNKNOWN';

export interface CiStep {
  id: string;
  stepNumber: number;
  stepName: string;
  status: string;
  conclusion: string | null;
  durationMs: number | null;
  failureReason: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface CiJob {
  id: string;
  externalJobId: string | null;
  jobName: string;
  status: string;
  conclusion: string | null;
  durationMs: number | null;
  failureReason: string | null;
  startedAt: string | null;
  completedAt: string | null;
  steps: CiStep[];
}

export interface CiWorkflowRun {
  id: string;
  externalWorkflowId: string | null;
  workflowName: string;
  externalRunId: string;
  runUrl: string | null;
  status: string;
  conclusion: string | null;
  durationMs: number | null;
  triggerEvent: string | null;
  commitHash: string | null;
  branch: string | null;
  pullRequestNumber: number | null;
  failureReason: string | null;
  startedAt: string | null;
  completedAt: string | null;
  jobs: CiJob[];
}

/** CI observation DTO — no credential or token fields. */
export interface CiObservationOperation {
  id: string;
  taskId: string;
  projectId: string;
  pullRequestOperationId: string;
  status: CiObservationStatus;
  provider: string;
  repositoryOwner: string;
  repositoryName: string;
  sourceBranch: string;
  targetBranch: string | null;
  commitHash: string | null;
  pullRequestNumber: number | null;
  overallStatus: CiOverallStatus;
  failureSummary: string | null;
  retryRecommendation: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  workflowRuns: CiWorkflowRun[];
  startedAt: string;
  completedAt: string | null;
  createdAt: string;
}
