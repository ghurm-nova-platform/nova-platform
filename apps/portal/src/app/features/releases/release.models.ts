export type ReleaseStatus =
  | 'DRAFT'
  | 'PREPARING'
  | 'READY'
  | 'PUBLISHED'
  | 'ARCHIVED'
  | 'FAILED';

export type VersionBump = 'MAJOR' | 'MINOR' | 'PATCH';

export type ReleaseContentType =
  | 'MERGE_OPERATION'
  | 'APPROVAL_DECISION'
  | 'PULL_REQUEST'
  | 'PATCH'
  | 'COMMIT';

export interface ReleaseTimelineEvent {
  eventType: string;
  at: string;
  detail: string | null;
}

export interface ReleaseContentItem {
  id: string;
  contentType: ReleaseContentType;
  referenceId: string | null;
  commitSha: string | null;
  sortOrder: number;
}

export interface ReleaseArtifactItem {
  id: string;
  artifactType: string;
  artifactUri: string;
  artifactHash: string | null;
  label: string | null;
  createdAt: string;
}

export interface ReleaseVersionView {
  id: string;
  semanticVersion: string;
  versionStrategy: string;
  bumpType: VersionBump | null;
  majorVersion: number;
  minorVersion: number;
  patchVersion: number;
  createdAt: string;
}

/** Release DTO — no secrets; manifest is immutable after READY. */
export interface Release {
  id: string;
  organizationId: string;
  projectId: string;
  releaseNumber: number;
  semanticVersion: string;
  releaseName: string;
  description: string | null;
  status: ReleaseStatus;
  versionStrategy: string;
  bumpType: VersionBump | null;
  contentFingerprint: string;
  manifestHash: string | null;
  manifestJson: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  createdBy: string;
  contents: ReleaseContentItem[];
  artifacts: ReleaseArtifactItem[];
  version: ReleaseVersionView | null;
  timeline: ReleaseTimelineEvent[];
  preparedAt: string | null;
  publishedAt: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReleaseRequest {
  projectId: string;
  releaseName: string;
  description?: string;
  bumpType?: VersionBump;
  semanticVersion?: string;
  mergeOperationIds?: string[];
  approvalDecisionIds?: string[];
  pullRequestIds?: string[];
  patchIds?: string[];
  commitShas?: string[];
}
