export type PromptStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type PromptVersionStatus = 'DRAFT' | 'PUBLISHED' | 'SUPERSEDED' | 'ARCHIVED';
export type PromptType =
  | 'CHAT'
  | 'SYSTEM'
  | 'CODING'
  | 'TRANSLATION'
  | 'SQL'
  | 'REPORT'
  | 'SUMMARIZATION'
  | 'CLASSIFICATION'
  | 'EXTRACTION'
  | 'RAG'
  | 'CUSTOM';

export type PromptVariableDataType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATETIME'
  | 'JSON'
  | 'ARRAY'
  | 'OBJECT';

export type DiffLineType = 'ADDED' | 'REMOVED' | 'UNCHANGED';

export interface Prompt {
  id: string;
  organizationId: string;
  projectId: string;
  name: string;
  description: string | null;
  promptType: PromptType;
  status: PromptStatus;
  tags: string[];
  currentDraftVersionId: string | null;
  currentDraftVersionNumber: number | null;
  publishedVersionId: string | null;
  publishedVersionNumber: number | null;
  version: number;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface PromptCreateRequest {
  name: string;
  description?: string | null;
  promptType: PromptType;
  content: string;
  changeSummary?: string | null;
  variables: PromptVariableRequest[];
  tags: string[];
}

export interface PromptUpdateRequest {
  name: string;
  description?: string | null;
  promptType: PromptType;
  tags: string[];
  version: number;
}

export interface PromptVersion {
  id: string;
  promptId: string;
  versionNumber: number;
  content: string;
  changeSummary: string | null;
  status: PromptVersionStatus;
  variables: PromptVariableResponse[];
  createdBy: string;
  createdAt: string;
  publishedBy: string | null;
  publishedAt: string | null;
}

export interface PromptVersionCreateRequest {
  changeSummary?: string | null;
}

export interface PromptVersionUpdateRequest {
  content: string;
  changeSummary?: string | null;
  variables: PromptVariableRequest[];
}

export interface PromptVariableRequest {
  name: string;
  description?: string | null;
  dataType: PromptVariableDataType;
  required: boolean;
  defaultValue?: string | null;
  sampleValue?: string | null;
}

export interface PromptVariableResponse {
  id: string;
  name: string;
  description: string | null;
  dataType: PromptVariableDataType;
  required: boolean;
  defaultValue: string | null;
  sampleValue: string | null;
}

export interface PromptValidateRequest {
  content: string;
  variables: PromptVariableRequest[];
}

export interface PromptValidateResponse {
  valid: boolean;
  detectedVariables: string[];
  errors: string[];
  warnings: string[];
}

export interface PromptPreviewRequest {
  content: string;
  variables: PromptVariableRequest[];
  values: Record<string, string>;
}

export interface PromptPreviewResponse {
  renderedContent: string;
  errors: string[];
  warnings: string[];
  missingRequiredVariables: string[];
}

export interface PromptCompareRequest {
  leftVersionId: string;
  rightVersionId: string;
}

export interface DiffLine {
  type: DiffLineType;
  lineNumber: number;
  content: string;
}

export interface PromptCompareResponse {
  leftVersionId: string;
  rightVersionId: string;
  leftContent: string;
  rightContent: string;
  diff: DiffLine[];
}

export interface PromptPublishRequest {
  reason?: string | null;
}

export interface PromptRollbackRequest {
  sourceVersionId: string;
  reason?: string | null;
}

export const PROMPT_TYPES: PromptType[] = [
  'CHAT',
  'SYSTEM',
  'CODING',
  'TRANSLATION',
  'SQL',
  'REPORT',
  'SUMMARIZATION',
  'CLASSIFICATION',
  'EXTRACTION',
  'RAG',
  'CUSTOM',
];

export const PROMPT_STATUSES: PromptStatus[] = ['DRAFT', 'PUBLISHED', 'ARCHIVED'];

export const PROMPT_VARIABLE_DATA_TYPES: PromptVariableDataType[] = [
  'STRING',
  'NUMBER',
  'BOOLEAN',
  'DATE',
  'DATETIME',
  'JSON',
  'ARRAY',
  'OBJECT',
];

export const PROMPT_VARIABLE_PATTERN = /\{\{\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*\}\}/g;

export function detectPromptVariables(content: string): string[] {
  const names = new Set<string>();
  for (const match of content.matchAll(PROMPT_VARIABLE_PATTERN)) {
    names.add(match[1]);
  }
  return [...names].sort();
}

export function parseTagsInput(input: string): string[] {
  return input
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0);
}

export function formatTagsInput(tags: string[]): string {
  return tags.join(', ');
}
