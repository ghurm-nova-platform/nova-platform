export type MergeStatus =
  | 'PENDING'
  | 'VALIDATING'
  | 'MERGING'
  | 'VERIFYING'
  | 'SUCCEEDED'
  | 'FAILED';

export type MergeMethod = 'MERGE' | 'SQUASH' | 'REBASE';

export type MergeValidationResult = 'PASSED' | 'FAILED' | 'SKIPPED' | 'ERROR';

export interface MergeTimelineEvent {
  eventType: string;
  detail: string | null;
  createdAt: string;
}

export interface MergeValidation {
  id: string;
  checkCode: string;
  expectedValue: string | null;
  actualValue: string | null;
  result: MergeValidationResult;
  failureReason: string | null;
  evaluatedAt: string;
}

export interface MergedCommit {
  hash: string;
  mergeMethod: MergeMethod;
  pullRequestNumber: number;
  pullRequestUrl: string | null;
  mergedAt: string | null;
  provider: string;
}

/** Merge operation DTO — no credential or secret fields. */
export interface MergeOperation {
  id: string;
  taskId: string;
  projectId: string;
  status: MergeStatus;
  mergeMethod: MergeMethod;
  approvalDecisionId: string;
  eligibleForMerge: boolean | null;
  evidenceFingerprint: string;
  decisionFingerprint: string;
  pullRequestNumber: number;
  repositoryOwner: string;
  repositoryName: string;
  validations: MergeValidation[];
  mergedCommit: MergedCommit | null;
  timeline: MergeTimelineEvent[];
  errorCode: string | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
  createdAt: string;
}
