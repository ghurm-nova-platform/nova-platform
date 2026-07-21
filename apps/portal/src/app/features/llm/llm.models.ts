export type LlmProviderType =
  | 'OLLAMA'
  | 'LLAMA_CPP'
  | 'VLLM'
  | 'OPENAI'
  | 'AZURE_OPENAI'
  | 'ANTHROPIC'
  | 'GEMINI'
  | 'MISTRAL'
  | 'DETERMINISTIC';

export type LlmProviderHealthStatus =
  | 'UNKNOWN'
  | 'HEALTHY'
  | 'DEGRADED'
  | 'UNAVAILABLE'
  | 'DISABLED';

export type LlmModelFamily =
  | 'LLAMA_3'
  | 'LLAMA_3_1'
  | 'LLAMA_3_2'
  | 'QWEN'
  | 'DEEPSEEK'
  | 'PHI'
  | 'GEMMA'
  | 'MISTRAL'
  | 'TINYLLAMA'
  | 'CODELLAMA'
  | 'CUSTOM';

export type LlmModelStatus =
  | 'REGISTERED'
  | 'DOWNLOADING'
  | 'INSTALLED'
  | 'LOADING'
  | 'READY'
  | 'UNLOADING'
  | 'STOPPED'
  | 'ERROR'
  | 'DISABLED';

export type LlmPromptCategory =
  | 'CHAT'
  | 'RAG'
  | 'SUMMARIZATION'
  | 'CLASSIFICATION'
  | 'TRANSLATION'
  | 'SQL_GENERATION'
  | 'CODE_GENERATION'
  | 'PR_REVIEW'
  | 'KNOWLEDGE_SEARCH'
  | 'WORKFLOW_AUTOMATION'
  | 'CUSTOM';

export type LlmMessageRole = 'SYSTEM' | 'USER' | 'ASSISTANT' | 'TOOL';

export type LlmConversationStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export interface LlmConfigResponse {
  enabled: boolean;
  defaultProvider: string;
  fallbackToDeterministic: boolean;
  timeoutSeconds: number;
  ollamaEnabled: boolean;
  llamacppEnabled: boolean;
  vllmEnabled: boolean;
}

export interface LlmMetricsSummaryResponse {
  metrics: Record<string, unknown>;
}

export interface LlmHealthResponse {
  providers: LlmProviderStatusView[];
}

export interface LlmProviderStatusView {
  providerType: LlmProviderType;
  status: LlmProviderHealthStatus;
  endpointUrl: string | null;
  lastHealthCheckAt: string | null;
  lastError: string | null;
}

export interface LlmModelView {
  id: string;
  organizationId: string;
  code: string;
  displayName: string;
  family: LlmModelFamily;
  providerType: LlmProviderType;
  status: LlmModelStatus;
  enabled: boolean;
  contextLength: number;
  endpointUrl: string | null;
  owner: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LlmRegisterModelRequest {
  code: string;
  displayName: string;
  family: LlmModelFamily;
  providerType: LlmProviderType;
  contextLength?: number | null;
  endpointUrl?: string | null;
  owner?: string | null;
  capabilitiesJson?: string | null;
  tagsJson?: string | null;
}

export interface LlmUpdateModelRequest {
  displayName?: string | null;
  family?: LlmModelFamily | null;
  contextLength?: number | null;
  endpointUrl?: string | null;
  owner?: string | null;
  capabilitiesJson?: string | null;
  tagsJson?: string | null;
}

export interface LlmChatMessageDto {
  role: string;
  content: string;
}

export interface LlmChatCompletionRequest {
  modelCode?: string | null;
  modelId?: string | null;
  conversationId?: string | null;
  messages?: LlmChatMessageDto[] | null;
  maxTokens?: number | null;
  temperature?: number | null;
  context?: string | null;
  knowledgeDocumentIds?: string[] | null;
}

export interface LlmTextCompletionRequest {
  modelCode?: string | null;
  modelId?: string | null;
  prompt: string;
  maxTokens?: number | null;
  temperature?: number | null;
}

export interface LlmBatchCompletionRequest {
  requests: LlmChatCompletionRequest[];
}

export interface LlmCompletionResponse {
  content: string;
  inputTokens: number;
  outputTokens: number;
  latencyMs: number;
  providerType: LlmProviderType;
  finishReason: string | null;
  conversationId: string | null;
  cancelToken: string | null;
}

export interface LlmPromptView {
  id: string;
  code: string;
  name: string;
  category: LlmPromptCategory;
  systemPrompt: string | null;
  userPromptTemplate: string;
  assistantPromptTemplate: string | null;
  variablesJson: string | null;
  templateVersion: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LlmCreatePromptRequest {
  code: string;
  name: string;
  category: LlmPromptCategory;
  systemPrompt?: string | null;
  userPromptTemplate: string;
  assistantPromptTemplate?: string | null;
  variablesJson?: string | null;
}

export interface LlmUpdatePromptRequest {
  name?: string | null;
  category?: LlmPromptCategory | null;
  systemPrompt?: string | null;
  userPromptTemplate?: string | null;
  assistantPromptTemplate?: string | null;
  variablesJson?: string | null;
  enabled?: boolean | null;
}

export interface LlmRenderPromptRequest {
  variables: Record<string, string>;
}

export interface LlmRenderPromptResponse {
  systemPrompt: string | null;
  userPrompt: string | null;
  assistantPrompt: string | null;
}

export interface LlmConversationView {
  id: string;
  modelId: string | null;
  projectId: string | null;
  title: string | null;
  status: LlmConversationStatus;
  summary: string | null;
  tokenUsageInput: number;
  tokenUsageOutput: number;
  createdAt: string;
  updatedAt: string;
}

export interface LlmMessageView {
  id: string;
  role: LlmMessageRole;
  content: string;
  tokenCount: number | null;
  sequenceNo: number;
  createdAt: string;
}

export interface LlmCreateConversationRequest {
  modelId?: string | null;
  projectId?: string | null;
  title?: string | null;
}

export interface LlmAppendMessageRequest {
  role: LlmMessageRole;
  content: string;
}

export interface LlmConfigEntryView {
  key: string;
  value: string;
  updatedAt: string;
}

export interface LlmSetConfigRequest {
  key: string;
  value: string;
}
