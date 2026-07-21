export type ExecutionStatus =
  | 'READY'
  | 'QUEUED'
  | 'STARTING'
  | 'DEPLOYING'
  | 'VERIFYING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type ExecutionProviderCode = 'LOCAL' | 'REST' | 'KUBERNETES' | 'ARGOCD' | 'HELM';

export interface ExecutionTimelineEvent {
  eventType: string;
  at: string;
  detail: string | null;
}

export interface ExecutionValidationCheck {
  id: string;
  checkCode: string;
  passed: boolean;
  message: string | null;
  createdAt: string;
}

export interface ExecutionStep {
  id: string;
  stepKey: string;
  stage: string;
  status: string;
  sortOrder: number;
  detail: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface ExecutionArtifact {
  id: string;
  artifactType: string;
  name: string;
  contentRef: string | null;
  checksum: string | null;
  createdAt: string;
}

export interface ExecutionResult {
  id: string;
  success: boolean;
  summary: string | null;
  providerResponseJson: string | null;
  createdAt: string;
}

export interface ExecutionLogEntry {
  id: string;
  level: string;
  message: string;
  createdAt: string;
}

export interface DeploymentExecution {
  id: string;
  organizationId: string;
  projectId: string;
  releaseId: string;
  environmentId: string;
  deploymentObservationId: string | null;
  provider: ExecutionProviderCode;
  status: ExecutionStatus;
  currentStep: string | null;
  currentStage: string | null;
  releaseManifestHash: string | null;
  releaseContentFingerprint: string | null;
  executionFingerprint: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
  triggeredBy: string | null;
  createdAt: string;
  updatedAt: string;
  errorCode: string | null;
  errorMessage: string | null;
  validations: ExecutionValidationCheck[];
  steps: ExecutionStep[];
  artifacts: ExecutionArtifact[];
  result: ExecutionResult | null;
  timeline: ExecutionTimelineEvent[];
}

export interface CreateExecutionRequest {
  releaseId: string;
  environmentId: string;
  deploymentObservationId?: string;
  provider?: ExecutionProviderCode;
  restDeployUrl?: string;
}
