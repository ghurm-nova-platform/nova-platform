export type AgentStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED';
export type AgentVisibility = 'PRIVATE' | 'PROJECT' | 'ORGANIZATION';

export interface Agent {
  id: string;
  organizationId: string;
  projectId: string;
  name: string;
  description: string | null;
  systemPrompt: string;
  modelProvider: string;
  modelName: string;
  temperature: number;
  maxTokens: number | null;
  status: AgentStatus;
  visibility: AgentVisibility;
  version: number;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentCreateRequest {
  name: string;
  description?: string | null;
  systemPrompt: string;
  modelProvider: string;
  modelName: string;
  temperature: number;
  maxTokens?: number | null;
  visibility: AgentVisibility;
}

export interface AgentUpdateRequest extends AgentCreateRequest {
  version: number;
}

export interface AgentStatusRequest {
  status: AgentStatus;
  version: number;
}

export const MODEL_PROVIDERS = [
  'OPENAI',
  'ANTHROPIC',
  'GOOGLE',
  'AZURE_OPENAI',
  'LOCAL',
] as const;
