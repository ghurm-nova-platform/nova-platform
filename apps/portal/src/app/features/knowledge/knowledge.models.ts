export type KnowledgeBaseStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type KnowledgeDocumentType = 'TEXT' | 'MARKDOWN' | 'PDF';

export type KnowledgeDocumentStatus =
  | 'UPLOADED'
  | 'PROCESSING'
  | 'READY'
  | 'FAILED'
  | 'ARCHIVED';

export const KNOWLEDGE_BASE_STATUSES: KnowledgeBaseStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];

export const KNOWLEDGE_DOCUMENT_STATUSES: KnowledgeDocumentStatus[] = [
  'UPLOADED',
  'PROCESSING',
  'READY',
  'FAILED',
  'ARCHIVED',
];

export const UPLOADABLE_DOCUMENT_TYPES: KnowledgeDocumentType[] = ['TEXT', 'MARKDOWN'];

export interface KnowledgeBase {
  id: string;
  organizationId: string;
  projectId: string;
  knowledgeKey: string;
  name: string;
  description: string | null;
  status: KnowledgeBaseStatus;
  embeddingProviderKey: string;
  embeddingModel: string;
  embeddingDimensions: number;
  chunkSize: number;
  chunkOverlap: number;
  defaultTopK: number;
  minimumScore: number | null;
  version: number;
  createdBy: string;
  updatedBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeBaseCreateRequest {
  knowledgeKey: string;
  name: string;
  description?: string | null;
  embeddingProviderKey: string;
  chunkSize: number;
  chunkOverlap: number;
  defaultTopK: number;
  minimumScore?: number | null;
}

export interface KnowledgeBaseUpdateRequest {
  version: number;
  name: string;
  description?: string | null;
  embeddingProviderKey?: string;
  chunkSize: number;
  chunkOverlap: number;
  defaultTopK: number;
  minimumScore?: number | null;
}

export interface KnowledgeBaseListParams {
  search?: string;
  status?: KnowledgeBaseStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface EmbeddingProvider {
  providerKey: string;
  model: string;
  dimensions: number;
}

export interface EmbeddingProvidersResponse {
  providers: EmbeddingProvider[];
}

export interface KnowledgeDocument {
  id: string;
  organizationId: string;
  projectId: string;
  knowledgeBaseId: string;
  documentKey: string;
  fileName: string;
  mediaType: string;
  documentType: KnowledgeDocumentType;
  status: KnowledgeDocumentStatus;
  contentHash: string;
  fileSizeBytes: number;
  extractedCharacterCount: number | null;
  chunkCount: number;
  ingestionErrorCode: string | null;
  version: number;
  createdBy: string;
  updatedBy: string | null;
  createdAt: string;
  updatedAt: string;
  processedAt: string | null;
}

export interface KnowledgeDocumentListParams {
  search?: string;
  status?: KnowledgeDocumentStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface KnowledgeChunk {
  id: string;
  documentId: string;
  chunkIndex: number;
  content: string;
  contentHash: string;
  characterStart: number;
  characterEnd: number;
  tokenEstimate: number | null;
  createdAt: string;
}

export interface KnowledgeChunkListParams {
  page?: number;
  size?: number;
  sort?: string;
}

export interface AgentKnowledgeAssignment {
  id: string;
  agentId: string;
  knowledgeBaseId: string;
  knowledgeKey: string;
  knowledgeBaseName: string;
  knowledgeBaseStatus: KnowledgeBaseStatus;
  enabled: boolean;
  topKOverride: number | null;
  minimumScoreOverride: number | null;
  version: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentKnowledgeAssignRequest {
  knowledgeBaseId: string;
  topKOverride?: number | null;
  minimumScoreOverride?: number | null;
}

export interface AgentKnowledgeAssignmentUpdateRequest {
  version: number;
  enabled?: boolean;
  topKOverride?: number | null;
  minimumScoreOverride?: number | null;
}
