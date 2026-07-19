export type DeploymentStatus =
  | 'PENDING'
  | 'STARTING'
  | 'RUNNING'
  | 'VERIFYING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED'
  | 'UNKNOWN';

export type DeploymentHealth =
  | 'HEALTHY'
  | 'WARNING'
  | 'DEGRADED'
  | 'FAILED'
  | 'UNKNOWN';

export interface DeploymentTimelineEvent {
  eventType: string;
  at: string;
  detail: string | null;
}

export interface HealthSnapshot {
  id: string;
  health: DeploymentHealth;
  message: string | null;
  observedAt: string;
}

export interface DeploymentArtifact {
  id: string;
  artifactType: string;
  artifactUri: string;
  artifactHash: string | null;
  label: string | null;
  createdAt: string;
}

export interface DeploymentEnvironment {
  id: string;
  code: string;
  name: string;
  environmentType: string;
  sortOrder: number;
  active: boolean;
}

export interface Deployment {
  id: string;
  organizationId: string;
  projectId: string;
  releaseId: string;
  environmentId: string;
  environmentCode: string;
  environmentName: string;
  customEnvironmentName: string | null;
  semanticVersion: string;
  releaseManifestHash: string | null;
  status: DeploymentStatus;
  health: DeploymentHealth;
  healthMessage: string | null;
  deploymentProvider: string;
  externalDeploymentKey: string | null;
  deploymentHash: string;
  triggeredBy: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
  logMetadata: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  artifacts: DeploymentArtifact[];
  healthHistory: HealthSnapshot[];
  timeline: DeploymentTimelineEvent[];
  createdAt: string;
  updatedAt: string;
}

export interface ObserveDeploymentRequest {
  releaseId: string;
  environment: string;
  customEnvironmentName?: string;
  status?: DeploymentStatus;
  health?: DeploymentHealth;
  healthMessage?: string;
  deploymentProvider: string;
  externalDeploymentKey?: string;
  startedAt?: string;
  finishedAt?: string;
  logMetadata?: string;
}
