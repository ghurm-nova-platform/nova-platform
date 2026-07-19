export type RollbackStatus =
  | 'DRAFT'
  | 'VALIDATING'
  | 'READY'
  | 'EXECUTING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED';

export type RollbackStrategy =
  | 'PREVIOUS_RELEASE'
  | 'PREVIOUS_STABLE'
  | 'SPECIFIC_RELEASE'
  | 'HOTFIX_ONLY'
  | 'CUSTOM';

export type RollbackRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type RollbackValidationResult = 'PENDING' | 'PASSED' | 'FAILED';

export interface RollbackTimelineEvent {
  eventType: string;
  at: string;
  detail: string | null;
}

export interface RollbackValidationCheck {
  id: string;
  checkCode: string;
  passed: boolean;
  message: string | null;
  createdAt: string;
}

export interface RollbackTarget {
  id: string;
  targetReleaseId: string;
  targetVersion: string;
  sortOrder: number;
  createdAt: string;
}

export interface RollbackPlan {
  id: string;
  currentReleaseId: string;
  targetReleaseId: string;
  deploymentId: string;
  environmentCode: string;
  strategy: RollbackStrategy;
  reason: string | null;
  riskLevel: RollbackRiskLevel;
  validationResult: RollbackValidationResult;
  validationMessage: string | null;
  immutable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Rollback {
  id: string;
  organizationId: string;
  projectId: string;
  releaseId: string;
  deploymentId: string;
  targetReleaseId: string;
  currentVersion: string;
  targetVersion: string;
  environmentId: string;
  environmentCode: string;
  status: RollbackStatus;
  strategy: RollbackStrategy;
  rollbackPlanHash: string;
  createdBy: string | null;
  createdAt: string;
  validatedAt: string | null;
  updatedAt: string;
  errorCode: string | null;
  errorMessage: string | null;
  plan: RollbackPlan;
  targets: RollbackTarget[];
  validations: RollbackValidationCheck[];
  timeline: RollbackTimelineEvent[];
}

export interface CreateRollbackRequest {
  releaseId: string;
  deploymentId: string;
  targetReleaseId: string;
  environment: string;
  strategy: RollbackStrategy;
  reason?: string;
  riskLevel?: RollbackRiskLevel;
}
