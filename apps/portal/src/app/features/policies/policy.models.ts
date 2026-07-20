export type PolicyStatus = 'ACTIVE' | 'DISABLED' | 'ARCHIVED';

export type PolicyType =
  | 'MINIMUM_APPROVALS'
  | 'CI_REQUIRED'
  | 'NO_FAILED_CHECKS'
  | 'SIGNED_COMMITS_REQUIRED'
  | 'SEMANTIC_VERSION_REQUIRED'
  | 'MANIFEST_INTEGRITY'
  | 'RELEASE_NOTES_REQUIRED'
  | 'DEPLOYMENT_OBSERVATION_EXISTS'
  | 'ROLLBACK_PLAN_EXISTS'
  | 'CUSTOM_EXPRESSION';

export type EvaluationMode = 'ALL_REQUIRED' | 'FIRST_FAILURE' | 'BEST_EFFORT';

export type PolicyDecision = 'PASSED' | 'FAILED' | 'WARNING' | 'SKIPPED' | 'ERROR';

export interface PolicyTimelineEvent {
  eventType: string;
  at: string;
  detail: string | null;
}

export interface PolicyEvidenceItem {
  id: string;
  evidenceKey: string;
  evidenceType: string;
  referenceId: string | null;
  passed: boolean;
  detail: string | null;
  createdAt: string;
}

export interface PolicyVersionView {
  id: string;
  versionNumber: number;
  policyType: PolicyType;
  evaluationMode: EvaluationMode;
  priority: number;
  createdAt: string;
}

export interface PolicyEvaluationView {
  id: string;
  releaseId: string;
  decision: PolicyDecision;
  evaluationHash: string;
  summary: string | null;
  completed: boolean;
  evidence: PolicyEvidenceItem[];
  evaluatedAt: string;
}

export interface Policy {
  id: string;
  organizationId: string;
  projectId: string;
  policyName: string;
  description: string | null;
  policyType: PolicyType;
  status: PolicyStatus;
  priority: number;
  evaluationMode: EvaluationMode;
  configuration: Record<string, unknown>;
  policyFingerprint: string;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
  latestEvaluation: PolicyEvaluationView | null;
  versions: PolicyVersionView[];
  timeline: PolicyTimelineEvent[];
}

export interface CreatePolicyRequest {
  projectId: string;
  policyName: string;
  description?: string;
  policyType: PolicyType;
  priority?: number;
  evaluationMode?: EvaluationMode;
  configuration?: Record<string, unknown>;
}

export interface EvaluatePolicyRequest {
  releaseId: string;
}
