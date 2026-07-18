export type GitStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED';

export interface GitValidation {
  valid: boolean;
  message: string;
}

export interface GitApplyResult {
  applied: boolean;
  details: string;
}

export interface GitBranch {
  id: string;
  branchName: string;
  baseRef: string;
  createdAt: string;
}

export interface GitCommit {
  id: string;
  commitHash: string;
  message: string;
  authorName: string;
  authorEmail: string;
  createdAt: string;
}

export interface TimelineEvent {
  phase: string;
  at: string;
  detail: string;
}

export interface GitOperation {
  id: string;
  taskId: string;
  runId: string;
  projectId: string;
  patchResultId: string;
  status: GitStatus;
  branchName: string;
  commitHash: string | null;
  patchHash: string;
  repositoryPath: string;
  baseRef: string;
  errorCode: string | null;
  validation: GitValidation;
  applyResult: GitApplyResult;
  branches: GitBranch[];
  commits: GitCommit[];
  timeline: TimelineEvent[];
  startedAt: string;
  completedAt: string | null;
  createdAt: string;
}
