export type RepairStatus =
  | 'PENDING'
  | 'COLLECTING'
  | 'ANALYZING'
  | 'GENERATING_PATCH'
  | 'VALIDATING'
  | 'SUCCEEDED'
  | 'FAILED';

export type RepairInputSourceType =
  | 'COMPILE'
  | 'TEST'
  | 'CI'
  | 'STATIC_ANALYSIS'
  | 'REVIEW'
  | 'FORMATTING'
  | 'DEPENDENCY'
  | 'COVERAGE';

export interface RepairTimelineEvent {
  phase: string;
  at: string;
  detail: string | null;
}

export interface RepairInput {
  id: string;
  sourceType: RepairInputSourceType;
  sourceRef: string | null;
  priority: number;
  detail: string;
}

export interface RepairAction {
  id: string;
  actionType: string;
  targetPath: string | null;
  description: string;
}

export interface RepairedFile {
  path: string;
  changeType: string | null;
  summary: string | null;
}

/** Repair operation DTO — no credential or secret fields. */
export interface RepairOperation {
  id: string;
  taskId: string;
  projectId: string;
  status: RepairStatus;
  attemptNumber: number;
  priorPatchResultId: string;
  patchResultId: string | null;
  reason: string;
  summary: string | null;
  confidence: number | null;
  repairedFiles: RepairedFile[];
  inputs: RepairInput[];
  actions: RepairAction[];
  timeline: RepairTimelineEvent[];
  errorCode: string | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
  createdAt: string;
}
