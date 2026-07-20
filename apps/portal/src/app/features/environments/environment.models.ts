export type EnvironmentStatus = 'ACTIVE' | 'DISABLED' | 'MAINTENANCE' | 'ARCHIVED';

export type EnvironmentType =
  | 'DEVELOPMENT'
  | 'TESTING'
  | 'QA'
  | 'STAGING'
  | 'PRODUCTION'
  | 'CUSTOM'
  | 'DR';

export interface EnvironmentTimelineEvent {
  eventType: string;
  at: string;
  detail: string | null;
}

export interface EnvironmentHistoryEntry {
  id: string;
  changeType: string;
  snapshotJson: string;
  createdBy: string | null;
  createdAt: string;
}

export interface EnvironmentLabelView {
  key: string;
  value: string;
  createdAt: string;
}

export interface EnvironmentVariableMetadataView {
  id: string;
  name: string;
  description: string | null;
  required: boolean;
  masked: boolean;
  scope: string;
  createdAt: string;
  updatedAt: string;
}

export interface ManagedEnvironment {
  id: string;
  organizationId: string;
  projectId: string;
  code: string;
  name: string;
  description: string | null;
  environmentType: EnvironmentType;
  status: EnvironmentStatus;
  active: boolean;
  region: string | null;
  provider: string | null;
  clusterName: string | null;
  namespaceName: string | null;
  cloudProvider: string | null;
  platform: string | null;
  ownerName: string | null;
  businessUnit: string | null;
  costCenter: string | null;
  tags: Record<string, string>;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
  labels: EnvironmentLabelView[];
  variables: EnvironmentVariableMetadataView[];
  timeline: EnvironmentTimelineEvent[];
  history: EnvironmentHistoryEntry[];
}

export interface CreateEnvironmentRequest {
  projectId: string;
  name: string;
  description?: string;
  environmentType: EnvironmentType | 'TEST';
  region?: string;
  provider?: string;
  clusterName?: string;
  namespaceName?: string;
  cloudProvider?: string;
  platform?: string;
  ownerName?: string;
  businessUnit?: string;
  costCenter?: string;
  tags?: Record<string, string>;
  labels?: { key: string; value: string }[];
  variables?: {
    name: string;
    description?: string;
    required: boolean;
    masked: boolean;
    scope: string;
  }[];
}

export interface UpdateEnvironmentRequest {
  name?: string;
  description?: string;
  region?: string;
  provider?: string;
  clusterName?: string;
  namespaceName?: string;
  cloudProvider?: string;
  platform?: string;
  ownerName?: string;
  businessUnit?: string;
  costCenter?: string;
  tags?: Record<string, string>;
  labels?: { key: string; value: string }[];
  variables?: {
    name: string;
    description?: string;
    required: boolean;
    masked: boolean;
    scope: string;
  }[];
}
