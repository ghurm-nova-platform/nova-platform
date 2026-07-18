export type ArtifactType =
  | 'SOURCE_FILE'
  | 'PATCH'
  | 'TEST'
  | 'DOCUMENTATION'
  | 'CONFIGURATION'
  | 'SQL_MIGRATION'
  | 'README';

export type ArtifactLanguage =
  | 'JAVA'
  | 'KOTLIN'
  | 'TYPESCRIPT'
  | 'JAVASCRIPT'
  | 'ANGULAR'
  | 'HTML'
  | 'CSS'
  | 'SCSS'
  | 'SQL'
  | 'ORACLE_SQL'
  | 'POSTGRESQL'
  | 'MYSQL'
  | 'PYTHON'
  | 'GO'
  | 'CSHARP'
  | 'MARKDOWN'
  | 'JSON'
  | 'YAML'
  | 'XML'
  | 'SHELL';

export interface CodeGenerationRequest {
  taskId: string;
}

export interface GeneratedArtifact {
  id: string;
  organizationId: string;
  projectId: string;
  runId: string;
  taskId: string;
  artifactType: ArtifactType;
  language: ArtifactLanguage;
  path: string;
  filename: string;
  content: string;
  sha256: string;
  tokensUsed: number | null;
  model: string | null;
  provider: string | null;
  generationTimeMs: number | null;
  createdAt: string;
}

export interface CodingResult {
  taskId: string;
  runId: string;
  projectId: string;
  summary: string;
  artifacts: GeneratedArtifact[];
  tokensUsed: number | null;
  model: string | null;
  provider: string | null;
  generationTimeMs: number | null;
  validated: boolean;
}

export interface DiffLine {
  kind: 'same' | 'add' | 'remove';
  text: string;
  leftNo: number | null;
  rightNo: number | null;
}

/** Lightweight line diff (no Monaco / VS Code). */
export function buildLineDiff(before: string, after: string): DiffLine[] {
  const a = (before ?? '').split(/\r?\n/);
  const b = (after ?? '').split(/\r?\n/);
  const n = a.length;
  const m = b.length;
  const dp: number[][] = Array.from({ length: n + 1 }, () => Array(m + 1).fill(0));
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      dp[i][j] = a[i] === b[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
    }
  }
  const lines: DiffLine[] = [];
  let i = 0;
  let j = 0;
  let leftNo = 1;
  let rightNo = 1;
  while (i < n && j < m) {
    if (a[i] === b[j]) {
      lines.push({ kind: 'same', text: a[i], leftNo: leftNo++, rightNo: rightNo++ });
      i++;
      j++;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      lines.push({ kind: 'remove', text: a[i], leftNo: leftNo++, rightNo: null });
      i++;
    } else {
      lines.push({ kind: 'add', text: b[j], leftNo: null, rightNo: rightNo++ });
      j++;
    }
  }
  while (i < n) {
    lines.push({ kind: 'remove', text: a[i++], leftNo: leftNo++, rightNo: null });
  }
  while (j < m) {
    lines.push({ kind: 'add', text: b[j++], leftNo: null, rightNo: rightNo++ });
  }
  return lines;
}
