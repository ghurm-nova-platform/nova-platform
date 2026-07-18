export type ExecutionStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface ExecutionTokenUsage {
  input: number;
  output: number;
  total: number;
}

export interface Citation {
  label: string;
  knowledgeBaseId: string;
  knowledgeBaseName: string;
  documentId: string;
  documentName: string;
  chunkIndex: number;
  score: number;
}

export interface AgentExecuteInput {
  message: string;
}

export interface AgentExecuteRequest {
  input: AgentExecuteInput;
  variables?: Record<string, string>;
  conversationId?: string | null;
  clientRequestId?: string | null;
}

export interface ExecutionModelMetadata {
  providerId: string;
  providerName: string;
  modelId: string;
  modelName: string;
  fallbackUsed: boolean;
  attemptCount: number;
}

export interface AgentExecuteResponse {
  executionId: string;
  status: ExecutionStatus;
  response: string;
  latencyMs: number;
  tokens: ExecutionTokenUsage;
  renderedPrompt?: string;
  errorMessage?: string | null;
  awaitingApproval?: boolean;
  pendingToolCallId?: string | null;
  citations?: Citation[];
  model?: ExecutionModelMetadata;
}

export interface Execution {
  id: string;
  organizationId: string;
  projectId: string;
  agentId: string;
  status: ExecutionStatus;
  response: string | null;
  latencyMs: number | null;
  tokens: ExecutionTokenUsage | null;
  renderedPrompt: string | null;
  errorMessage: string | null;
  conversationId: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
}

export interface ExecutionListParams {
  agentId?: string;
  status?: ExecutionStatus;
  page?: number;
  size?: number;
  sort?: string;
}
