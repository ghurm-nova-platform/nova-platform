export type ContentFormat =
  | 'MARKDOWN'
  | 'PLAIN_TEXT'
  | 'HTML'
  | 'CODE'
  | 'JSON'
  | 'YAML'
  | 'SQL'
  | 'XML';

export type KnowledgeType =
  | 'PROJECT'
  | 'CODE'
  | 'DOCUMENTATION'
  | 'ADR'
  | 'PULL_REQUEST'
  | 'RELEASE'
  | 'DEPLOYMENT'
  | 'PIPELINE'
  | 'TEST'
  | 'BUG'
  | 'FIX'
  | 'DECISION'
  | 'BEST_PRACTICE'
  | 'RUNBOOK'
  | 'API';

export type Category =
  | 'Architecture'
  | 'Backend'
  | 'Frontend'
  | 'Database'
  | 'Infrastructure'
  | 'Security'
  | 'Testing'
  | 'Deployment'
  | 'Operations'
  | 'AI'
  | 'General';

export type DocumentStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export type Visibility = 'PRIVATE' | 'PROJECT' | 'ORGANIZATION' | 'PUBLIC';

export type RelationType =
  | 'REFERENCES'
  | 'RELATED_ADR'
  | 'RELATED_PR'
  | 'RELATED_RELEASE'
  | 'RELATED_DEPLOYMENT'
  | 'RELATED_PROJECT'
  | 'RELATED_DECISION';

export interface KnowledgeEngineConfigResponse {
  enabled: boolean;
  cacheEnabled: boolean;
  cacheTtlSeconds: number;
  chunkSize: number;
  chunkOverlap: number;
}

export interface CreateDocumentRequest {
  projectId?: string | null;
  title: string;
  summary?: string | null;
  content: string;
  contentFormat: ContentFormat;
  knowledgeType: KnowledgeType;
  category: Category;
  visibility?: Visibility | null;
  tags?: string[] | null;
}

export interface UpdateDocumentRequest {
  projectId?: string | null;
  title?: string | null;
  summary?: string | null;
  content?: string | null;
  contentFormat?: ContentFormat | null;
  knowledgeType?: KnowledgeType | null;
  category?: Category | null;
  visibility?: Visibility | null;
  tags?: string[] | null;
}

export interface ImportDocumentRequest {
  projectId?: string | null;
  title: string;
  summary?: string | null;
  content: string;
  contentFormat?: ContentFormat | null;
  knowledgeType?: KnowledgeType | null;
  category?: Category | null;
  visibility?: Visibility | null;
  tags?: string[] | null;
  importFormat?: string | null;
}

export interface RelateDocumentRequest {
  relationType: RelationType;
  targetDocumentId?: string | null;
  targetRefId?: string | null;
  targetRefType?: string | null;
}

export interface DocumentSummary {
  id: string;
  organizationId: string;
  projectId: string | null;
  title: string;
  summary: string | null;
  contentFormat: ContentFormat;
  knowledgeType: KnowledgeType;
  category: Category;
  status: DocumentStatus;
  visibility: Visibility;
  authorId: string;
  version: number;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface RelationView {
  id: string;
  relationType: RelationType;
  targetDocumentId: string | null;
  targetRefId: string | null;
  targetRefType: string | null;
  createdAt: string;
}

export interface AttachmentView {
  id: string;
  fileName: string;
  contentType: string;
  storageRef: string;
  sizeBytes: number;
  createdAt: string;
}

export interface DocumentDetail extends DocumentSummary {
  content: string;
  relations: RelationView[];
  attachments: AttachmentView[];
}

export interface SearchResult {
  id: string;
  title: string;
  summary: string | null;
  knowledgeType: KnowledgeType;
  category: Category;
  visibility: Visibility;
  projectId: string | null;
  authorId: string;
  tags: string[];
  matchedSnippet: string | null;
  updatedAt: string;
}

export interface MemoryDocument {
  id: string;
  title: string;
  summary: string | null;
  knowledgeType: KnowledgeType;
  category: Category;
  projectId: string | null;
  updatedAt: string;
}

export interface KnowledgeSearchParams {
  q?: string;
  tag?: string;
  category?: Category;
  projectId?: string;
  authorId?: string;
  visibility?: Visibility;
  knowledgeType?: KnowledgeType;
  from?: string;
  to?: string;
}

export interface KnowledgeMemoryParams {
  projectId?: string;
  types?: KnowledgeType[];
  limit?: number;
}

export const KNOWLEDGE_CATEGORIES: Category[] = [
  'Architecture',
  'Backend',
  'Frontend',
  'Database',
  'Infrastructure',
  'Security',
  'Testing',
  'Deployment',
  'Operations',
  'AI',
  'General',
];

export const KNOWLEDGE_TYPES: KnowledgeType[] = [
  'PROJECT',
  'CODE',
  'DOCUMENTATION',
  'ADR',
  'PULL_REQUEST',
  'RELEASE',
  'DEPLOYMENT',
  'PIPELINE',
  'TEST',
  'BUG',
  'FIX',
  'DECISION',
  'BEST_PRACTICE',
  'RUNBOOK',
  'API',
];

export const VISIBILITY_OPTIONS: Visibility[] = [
  'PRIVATE',
  'PROJECT',
  'ORGANIZATION',
  'PUBLIC',
];
