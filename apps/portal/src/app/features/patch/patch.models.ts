export type PatchStatus = 'VALID' | 'INVALID';
export type PatchChangeType = 'ADD' | 'MODIFY' | 'DELETE' | 'RENAME';

export interface PatchStatistics {
  filesChanged: number;
  insertions: number;
  deletions: number;
  patchSize: number;
}

export interface PatchValidation {
  valid: boolean;
  message: string;
}

export interface PatchFile {
  id: string;
  path: string;
  oldPath: string | null;
  newPath: string | null;
  changeType: PatchChangeType;
  insertions: number;
  deletions: number;
  patchExcerpt: string | null;
}

export interface PatchResult {
  id: string;
  taskId: string;
  runId: string;
  projectId: string;
  summary: string;
  status: PatchStatus;
  statistics: PatchStatistics;
  patch: string;
  files: PatchFile[];
  artifacts: {
    artifactId: string;
    path: string;
    filename: string;
    language: string;
    sha256: string;
  }[];
  validation: PatchValidation;
  tokensUsed: number | null;
  model: string | null;
  provider: string | null;
  generationTimeMs: number | null;
  createdAt: string;
}
