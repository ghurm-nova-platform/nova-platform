export type PullRequestStatus =
  | 'PENDING'
  | 'VALIDATING'
  | 'PUSHING'
  | 'PUSHED'
  | 'CREATING_PR'
  | 'SUCCEEDED'
  | 'FAILED';

export type RemotePushStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED';

export interface PullRequestValidation {
  valid: boolean;
  message: string;
}

export interface RemotePushResult {
  id: string;
  remoteName: string;
  sourceBranch: string;
  localCommitHash: string;
  remoteCommitHash: string | null;
  status: RemotePushStatus;
  errorCode: string | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface PullRequestRecord {
  id: string;
  provider: string;
  externalId: string;
  pullRequestNumber: number;
  pullRequestUrl: string;
  title: string;
  sourceBranch: string;
  targetBranch: string;
  state: string;
  createdAt: string;
}

export interface TimelineEvent {
  phase: string;
  at: string;
  detail: string;
}

/** Pull request operation DTO — no credential or token fields. */
export interface PullRequestOperation {
  id: string;
  taskId: string;
  projectId: string;
  gitOperationId: string;
  patchResultId: string | null;
  status: PullRequestStatus;
  provider: string;
  repositoryOwner: string;
  repositoryName: string;
  remoteName: string;
  remoteUrl: string;
  sourceBranch: string;
  targetBranch: string;
  localCommitHash: string;
  remoteCommitHash: string | null;
  patchHash: string;
  pullRequestNumber: number | null;
  pullRequestUrl: string | null;
  pullRequestTitle: string | null;
  errorCode: string | null;
  validation: PullRequestValidation;
  remotePush: RemotePushResult | null;
  pullRequestRecord: PullRequestRecord | null;
  timeline: TimelineEvent[];
  startedAt: string;
  completedAt: string | null;
  createdAt: string;
}
