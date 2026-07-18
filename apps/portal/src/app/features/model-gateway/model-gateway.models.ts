export type AiProviderStatus = 'DRAFT' | 'ACTIVE' | 'DISABLED' | 'ARCHIVED';

export type AiProviderType =
  | 'DETERMINISTIC_LOCAL'
  | 'OPENAI'
  | 'AZURE_OPENAI'
  | 'ANTHROPIC'
  | 'GOOGLE_GEMINI'
  | 'AWS_BEDROCK'
  | 'CUSTOM_MANAGED';

export type AiModelStatus = 'DRAFT' | 'ACTIVE' | 'DISABLED' | 'DEPRECATED' | 'ARCHIVED';

export type AiModelSource = 'MANUAL' | 'PROVIDER_SYNC';

export type AiModelType =
  | 'TEXT_GENERATION'
  | 'CHAT'
  | 'REASONING'
  | 'EMBEDDING'
  | 'MULTIMODAL';

export type AiModelCapability =
  | 'CHAT'
  | 'EMBEDDINGS'
  | 'VISION'
  | 'IMAGE_GENERATION'
  | 'IMAGE_UNDERSTANDING'
  | 'AUDIO_INPUT'
  | 'AUDIO_OUTPUT'
  | 'TRANSCRIPTION'
  | 'TEXT_TO_SPEECH'
  | 'FUNCTION_CALLING'
  | 'TOOL_CALLING'
  | 'PARALLEL_TOOL_CALLING'
  | 'JSON_MODE'
  | 'STRUCTURED_OUTPUT'
  | 'REASONING'
  | 'STREAMING'
  | 'BATCH'
  | 'FINE_TUNING';

export type AssignmentRole = 'PRIMARY' | 'FALLBACK';

export type RoutingPolicyStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type RoutingStrategy = 'FIXED_PRIMARY' | 'PRIORITY_FALLBACK' | 'CAPABILITY_BASED';

export type EndpointProfile = 'OPENAI_PUBLIC' | 'AZURE_OPENAI_RESOURCE';

export type ConnectionTestStatus = 'NEVER' | 'SUCCESS' | 'FAILED';

export type ModelSyncStatus = 'SUCCESS' | 'STALE' | 'FAILED' | 'UNSUPPORTED';

export type ProviderSecretStatus = 'ACTIVE' | 'ROTATED' | 'REVOKED' | 'ARCHIVED';

export const AI_PROVIDER_STATUSES: AiProviderStatus[] = ['DRAFT', 'ACTIVE', 'DISABLED', 'ARCHIVED'];

export const AI_PROVIDER_TYPES: AiProviderType[] = [
  'DETERMINISTIC_LOCAL',
  'OPENAI',
  'AZURE_OPENAI',
  'ANTHROPIC',
  'GOOGLE_GEMINI',
  'AWS_BEDROCK',
  'CUSTOM_MANAGED',
];

export const PROVIDER_SECRET_TYPES: AiProviderType[] = AI_PROVIDER_TYPES.filter(
  (type) => type !== 'DETERMINISTIC_LOCAL',
);

export const PROVIDER_SECRET_STATUSES: ProviderSecretStatus[] = ['ACTIVE', 'ROTATED', 'REVOKED', 'ARCHIVED'];

export const ENDPOINT_PROFILES: EndpointProfile[] = ['OPENAI_PUBLIC', 'AZURE_OPENAI_RESOURCE'];

export const AI_MODEL_STATUSES: AiModelStatus[] = [
  'DRAFT',
  'ACTIVE',
  'DISABLED',
  'DEPRECATED',
  'ARCHIVED',
];

export const AI_MODEL_SOURCES: AiModelSource[] = ['MANUAL', 'PROVIDER_SYNC'];

export const AI_MODEL_TYPES: AiModelType[] = [
  'TEXT_GENERATION',
  'CHAT',
  'REASONING',
  'EMBEDDING',
  'MULTIMODAL',
];

export const AI_MODEL_CAPABILITIES: AiModelCapability[] = [
  'CHAT',
  'EMBEDDINGS',
  'VISION',
  'IMAGE_GENERATION',
  'IMAGE_UNDERSTANDING',
  'AUDIO_INPUT',
  'AUDIO_OUTPUT',
  'TRANSCRIPTION',
  'TEXT_TO_SPEECH',
  'FUNCTION_CALLING',
  'TOOL_CALLING',
  'PARALLEL_TOOL_CALLING',
  'JSON_MODE',
  'STRUCTURED_OUTPUT',
  'REASONING',
  'STREAMING',
  'BATCH',
  'FINE_TUNING',
];

export const ASSIGNMENT_ROLES: AssignmentRole[] = ['PRIMARY', 'FALLBACK'];

export const ROUTING_POLICY_STATUSES: RoutingPolicyStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];

export const ROUTING_STRATEGIES: RoutingStrategy[] = [
  'FIXED_PRIMARY',
  'PRIORITY_FALLBACK',
  'CAPABILITY_BASED',
];

export interface ModelProvider {
  id: string;
  organizationId?: string;
  providerKey: string;
  name: string;
  description: string | null;
  providerType: AiProviderType;
  adapterKey: string;
  credentialReference?: string | null;
  region: string | null;
  endpointProfile?: EndpointProfile | null;
  azureResourceName?: string | null;
  azureApiVersion?: string | null;
  lastConnectionTestStatus?: ConnectionTestStatus;
  lastConnectionTestAt?: string | null;
  lastConnectionTestErrorCode?: string | null;
  lastModelSyncAt?: string | null;
  lastModelSyncStatus?: ModelSyncStatus | null;
  lastModelSyncErrorCode?: string | null;
  lastModelSyncDiscoveredCount?: number | null;
  lastModelSyncCreatedCount?: number | null;
  lastModelSyncUpdatedCount?: number | null;
  lastModelSyncUnchangedCount?: number | null;
  status: AiProviderStatus;
  requestTimeoutSeconds: number;
  maxConcurrentRequests: number;
  maxRetries: number;
  retryBackoffMs: number;
  version: number;
  createdBy?: string;
  updatedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ModelProviderCreateRequest {
  providerKey: string;
  name: string;
  description?: string | null;
  providerType: AiProviderType;
  adapterKey: string;
  credentialReference?: string | null;
  region?: string | null;
  endpointProfile?: EndpointProfile | null;
  azureResourceName?: string | null;
  azureApiVersion?: string | null;
  requestTimeoutSeconds: number;
  maxConcurrentRequests: number;
  maxRetries: number;
  retryBackoffMs: number;
}

export interface ModelProviderUpdateRequest {
  version: number;
  name: string;
  description?: string | null;
  credentialReference?: string | null;
  region?: string | null;
  endpointProfile?: EndpointProfile | null;
  azureResourceName?: string | null;
  azureApiVersion?: string | null;
  requestTimeoutSeconds: number;
  maxConcurrentRequests: number;
  maxRetries: number;
  retryBackoffMs: number;
}

export interface ConnectionTestResponse {
  status: ConnectionTestStatus;
  errorCode: string | null;
  testedAt: string;
}

export interface ProviderSecret {
  id: string;
  secretKey: string;
  name: string;
  description: string | null;
  providerType: AiProviderType;
  status: ProviderSecretStatus;
  credentialReference: string;
  algorithm: string;
  keyVersion: number;
  last4: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  rotatedAt: string | null;
  revokedAt: string | null;
}

export interface ProviderSecretCreateRequest {
  secretKey: string;
  name: string;
  description?: string | null;
  providerType: AiProviderType;
  secret: string;
}

export interface ProviderSecretRotateRequest {
  secret: string;
}

export interface ProviderSecretListParams {
  search?: string;
  status?: ProviderSecretStatus;
  providerType?: AiProviderType;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ModelProviderListParams {
  search?: string;
  status?: AiProviderStatus;
  providerType?: AiProviderType;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ProviderAdapter {
  adapterKey: string;
  tools: boolean;
  knowledgeContext: boolean;
  jsonOutput: boolean;
  systemMessages: boolean;
  streaming: boolean;
}

export interface ProviderAdaptersResponse {
  adapters: ProviderAdapter[];
}

/** @deprecated Prefer ProviderAdaptersResponse */
export interface AdapterKeysResponse {
  adapterKeys: string[];
}

export interface AiModel {
  id: string;
  organizationId: string;
  providerId: string;
  modelKey: string;
  providerModelId: string;
  displayName: string;
  description: string | null;
  modelType: AiModelType;
  status: AiModelStatus;
  contextWindowTokens: number;
  maxOutputTokens: number;
  supportsTools: boolean;
  supportsKnowledgeContext: boolean;
  supportsJsonOutput: boolean;
  supportsStreaming: boolean;
  supportsSystemMessages: boolean;
  inputCostPerMillion: number | null;
  outputCostPerMillion: number | null;
  currencyCode: string | null;
  version: number;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface AiModelCreateRequest {
  modelKey: string;
  providerModelId: string;
  displayName: string;
  description?: string | null;
  modelType: AiModelType;
  contextWindowTokens: number;
  maxOutputTokens: number;
  supportsTools: boolean;
  supportsKnowledgeContext: boolean;
  supportsJsonOutput: boolean;
  supportsStreaming: boolean;
  supportsSystemMessages: boolean;
  inputCostPerMillion?: number | null;
  outputCostPerMillion?: number | null;
  currencyCode?: string | null;
}

export interface AiModelUpdateRequest {
  version: number;
  displayName: string;
  description?: string | null;
  providerModelId: string;
  contextWindowTokens: number;
  maxOutputTokens: number;
  supportsTools: boolean;
  supportsKnowledgeContext: boolean;
  supportsJsonOutput: boolean;
  supportsStreaming: boolean;
  supportsSystemMessages: boolean;
  inputCostPerMillion?: number | null;
  outputCostPerMillion?: number | null;
  currencyCode?: string | null;
}

export interface AiModelListParams {
  search?: string;
  status?: AiModelStatus;
  modelType?: AiModelType;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ProjectModelAssignment {
  id: string;
  projectId: string;
  modelId: string;
  modelKey: string;
  displayName: string;
  providerId: string;
  providerName: string;
  modelStatus: AiModelStatus;
  enabled: boolean;
  isDefault: boolean;
  maximumInputTokensOverride: number | null;
  maximumOutputTokensOverride: number | null;
  dailyRequestLimit: number | null;
  monthlyRequestLimit: number | null;
  version: number;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectModelAssignRequest {
  modelId: string;
  enabled?: boolean;
  isDefault?: boolean;
  maximumInputTokensOverride?: number | null;
  maximumOutputTokensOverride?: number | null;
  dailyRequestLimit?: number | null;
  monthlyRequestLimit?: number | null;
}

export interface ProjectModelUpdateRequest {
  version: number;
  enabled: boolean;
  isDefault: boolean;
  maximumInputTokensOverride?: number | null;
  maximumOutputTokensOverride?: number | null;
  dailyRequestLimit?: number | null;
  monthlyRequestLimit?: number | null;
}

export interface AgentModelAssignment {
  id: string;
  agentId: string;
  modelId: string;
  modelKey: string;
  displayName: string;
  providerId: string;
  providerName: string;
  modelStatus: AiModelStatus;
  priority: number;
  assignmentRole: AssignmentRole;
  enabled: boolean;
  temperatureOverride: number | null;
  maximumOutputTokensOverride: number | null;
  version: number;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentModelAssignRequest {
  modelId: string;
  assignmentRole: AssignmentRole;
  priority: number;
  enabled?: boolean;
  temperatureOverride?: number | null;
  maximumOutputTokensOverride?: number | null;
}

export interface AgentModelUpdateRequest {
  version: number;
  priority: number;
  enabled: boolean;
  temperatureOverride?: number | null;
  maximumOutputTokensOverride?: number | null;
}

export interface ModelRoutingPolicy {
  id: string;
  organizationId: string;
  projectId: string;
  agentId: string | null;
  policyKey: string;
  name: string;
  description: string | null;
  status: RoutingPolicyStatus;
  strategy: RoutingStrategy;
  fallbackEnabled: boolean;
  retryEnabled: boolean;
  maximumProviderAttempts: number;
  maximumTotalDurationMs: number;
  requireToolSupport: boolean;
  requireKnowledgeSupport: boolean;
  version: number;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface ModelRoutingPolicyCreateRequest {
  policyKey: string;
  name: string;
  description?: string | null;
  agentId?: string | null;
  strategy: RoutingStrategy;
  fallbackEnabled: boolean;
  retryEnabled: boolean;
  maximumProviderAttempts: number;
  maximumTotalDurationMs: number;
  requireToolSupport: boolean;
  requireKnowledgeSupport: boolean;
}

export interface ModelRoutingPolicyUpdateRequest {
  version: number;
  name: string;
  description?: string | null;
  agentId?: string | null;
  strategy: RoutingStrategy;
  fallbackEnabled: boolean;
  retryEnabled: boolean;
  maximumProviderAttempts: number;
  maximumTotalDurationMs: number;
  requireToolSupport: boolean;
  requireKnowledgeSupport: boolean;
}

export interface ModelRoutingPolicyListParams {
  search?: string;
  status?: RoutingPolicyStatus;
  agentId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ModelUsageDaily {
  id: string;
  projectId: string;
  providerId: string;
  providerName: string;
  modelId: string;
  modelKey: string;
  displayName: string;
  usageDate: string;
  requestCount: number;
  successfulRequestCount: number;
  failedRequestCount: number;
  inputTokens: number;
  outputTokens: number;
  estimatedCost: number | null;
  currencyCode: string | null;
  updatedAt: string;
}

export interface ModelUsageListParams {
  from?: string;
  to?: string;
  providerId?: string;
  modelId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ModelUsageSummary {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  estimatedCost: number | null;
  currencyCode: string | null;
}

export interface ModelUsageResponse {
  summary: ModelUsageSummary;
  daily: ModelUsageDaily[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CatalogModelCapability {
  capability: AiModelCapability;
  enabled: boolean;
  metadataJson: string | null;
  createdAt: string;
}

export interface CatalogModelAlias {
  id: string;
  modelId: string;
  alias: string;
  normalizedAlias: string;
  createdAt: string;
}

export interface CatalogModel {
  id: string;
  providerId: string;
  providerName: string;
  modelKey: string;
  providerModelId: string;
  displayName: string;
  description: string | null;
  modelType: AiModelType;
  status: AiModelStatus;
  source: AiModelSource;
  modelFamily: string | null;
  modelVersion: string | null;
  contextWindowTokens: number;
  contextWindow: number | null;
  maxInputTokens: number | null;
  maxOutputTokens: number;
  defaultTemperature: number | null;
  defaultTopP: number | null;
  defaultMaxOutputTokens: number | null;
  supportsTools: boolean;
  supportsKnowledgeContext: boolean;
  supportsJsonOutput: boolean;
  supportsStreaming: boolean;
  supportsSystemMessages: boolean;
  inputCostPerMillion: number | null;
  outputCostPerMillion: number | null;
  currency: string | null;
  discoveredAt: string | null;
  lastSyncedAt: string | null;
  lastSeenAt: string | null;
  capabilities: CatalogModelCapability[];
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CatalogModelCreateRequest {
  providerId: string;
  modelKey: string;
  providerModelId: string;
  displayName: string;
  description?: string | null;
  modelType: AiModelType;
  modelFamily?: string | null;
  modelVersion?: string | null;
  contextWindowTokens: number;
  maxInputTokens?: number | null;
  maxOutputTokens: number;
  defaultTemperature?: number | null;
  defaultTopP?: number | null;
  defaultMaxOutputTokens?: number | null;
  supportsKnowledgeContext?: boolean;
  supportsSystemMessages?: boolean;
  inputCostPerMillion?: number | null;
  outputCostPerMillion?: number | null;
  currency?: string | null;
  capabilities?: AiModelCapability[];
}

export interface CatalogModelUpdateRequest {
  displayName: string;
  description?: string | null;
  modelFamily?: string | null;
  modelVersion?: string | null;
  contextWindowTokens: number;
  maxInputTokens?: number | null;
  maxOutputTokens: number;
  defaultTemperature?: number | null;
  defaultTopP?: number | null;
  defaultMaxOutputTokens?: number | null;
  supportsKnowledgeContext?: boolean;
  supportsSystemMessages?: boolean;
  inputCostPerMillion?: number | null;
  outputCostPerMillion?: number | null;
  currency?: string | null;
  version: number;
}

export interface CatalogCapabilityInput {
  capability: AiModelCapability;
  enabled?: boolean;
  metadataJson?: string | null;
}

export interface CatalogModelListParams {
  search?: string;
  status?: AiModelStatus;
  source?: AiModelSource;
  capability?: AiModelCapability;
  providerId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CatalogSyncResult {
  providerId: string;
  discoveredCount: number;
  createdCount: number;
  updatedCount: number;
  unchangedCount: number;
  stale: boolean;
  status: ModelSyncStatus;
  errorCode: string | null;
  syncedAt: string;
}

export interface CatalogAliasCreateRequest {
  alias: string;
}
