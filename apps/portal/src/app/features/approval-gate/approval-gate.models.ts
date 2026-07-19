export type ApprovalDecisionValue =
  | 'PENDING'
  | 'ELIGIBLE'
  | 'BLOCKED'
  | 'REQUIRES_HUMAN_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'SUPERSEDED'
  | 'INVALIDATED'
  | 'ERROR';

export type ApprovalOperationStatus =
  | 'PENDING'
  | 'COLLECTING'
  | 'EVALUATING'
  | 'WAITING_FOR_HUMAN'
  | 'SUCCEEDED'
  | 'FAILED';

export type ApprovalRequirementResult =
  | 'PASSED'
  | 'FAILED'
  | 'NOT_APPLICABLE'
  | 'PENDING'
  | 'ERROR';

export type ApprovalHumanActionType = 'APPROVE' | 'REJECT' | 'WITHDRAW_APPROVAL';

export type ApprovalEvidenceType =
  | 'REVIEW'
  | 'TESTING'
  | 'PATCH'
  | 'GIT'
  | 'PULL_REQUEST'
  | 'CI'
  | 'REPAIR'
  | 'POLICY';

export type ApprovalPolicyStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'SUPERSEDED';

export type ApprovalSeverity = 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface ApprovalTimelineEvent {
  eventType: string;
  detail: string | null;
  actorUserId: string | null;
  createdAt: string;
}

export interface ApprovalRequirement {
  id: string;
  ruleCode: string;
  description: string | null;
  expectedValue: string | null;
  actualValue: string | null;
  result: ApprovalRequirementResult;
  blocking: boolean;
  severity: ApprovalSeverity;
  failureReason: string | null;
  evaluatedAt: string;
}

export interface ApprovalEvidenceRef {
  id: string;
  evidenceType: ApprovalEvidenceType;
  sourceOperationId: string;
  sourceResultId: string | null;
  sourceVersion: string | null;
  sourceHash: string | null;
  observedStatus: string | null;
  observedValue: string | null;
}

export interface ApprovalHumanActionView {
  id: string;
  actorUserId: string;
  actorDisplayName: string | null;
  action: ApprovalHumanActionType;
  commentText: string | null;
  evidenceFingerprint: string;
  createdAt: string;
}

export interface ReviewEvidenceSummary {
  approved: boolean;
  score: number | null;
  criticalFindings: number;
  highFindings: number;
}

export interface TestingEvidenceSummary {
  validated: boolean;
  coverageEstimate: number | null;
  summary: string | null;
}

export interface RepairEvidenceSummary {
  status: string | null;
  attemptNumber: number | null;
  patchResultId: string | null;
}

/** Approval decision DTO — no credential or secret fields. */
export interface ApprovalDecision {
  id: string;
  taskId: string;
  projectId: string;
  operationId: string;
  operationStatus: ApprovalOperationStatus;
  decision: ApprovalDecisionValue;
  eligibleForMerge: boolean;
  stale: boolean;
  policyId: string;
  policyName: string;
  policyVersion: number;
  evidenceFingerprint: string;
  decisionFingerprint: string;
  patchResultId: string;
  patchHash: string;
  gitOperationId: string;
  commitHash: string;
  pullRequestOperationId: string;
  pullRequestNumber: number;
  pullRequestUrl: string | null;
  ciObservationOperationId: string | null;
  ciOverallStatus: string | null;
  ciCommitHash: string | null;
  repairOperationId: string | null;
  reviewSummary: ReviewEvidenceSummary | null;
  testingSummary: TestingEvidenceSummary | null;
  repairSummary: RepairEvidenceSummary | null;
  requiredHumanApprovals: number;
  receivedHumanApprovals: number;
  rejectionCount: number;
  reasonSummary: string | null;
  validUntil: string | null;
  requirements: ApprovalRequirement[];
  evidence: ApprovalEvidenceRef[];
  humanActions: ApprovalHumanActionView[];
  timeline: ApprovalTimelineEvent[];
  errorCode: string | null;
  errorMessage: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  supersededAt: string | null;
  invalidatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApprovalHumanActionRequest {
  comment?: string | null;
  idempotencyKey?: string | null;
}
