export type ToolStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type ToolType = 'BUILT_IN' | 'HTTP' | 'WEBHOOK' | 'INTEGRATION';

export type ToolCallStatus =
  | 'REQUESTED'
  | 'APPROVAL_REQUIRED'
  | 'APPROVED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'REJECTED'
  | 'CANCELLED';

export const TOOL_STATUSES: ToolStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];

export const TOOL_TYPES: ToolType[] = ['BUILT_IN', 'HTTP', 'WEBHOOK', 'INTEGRATION'];

export const TOOL_CALL_STATUSES: ToolCallStatus[] = [
  'REQUESTED',
  'APPROVAL_REQUIRED',
  'APPROVED',
  'RUNNING',
  'COMPLETED',
  'FAILED',
  'REJECTED',
  'CANCELLED',
];

export interface ToolDefinition {
  id: string;
  organizationId: string;
  projectId: string;
  toolKey: string;
  name: string;
  description: string | null;
  toolType: ToolType;
  executorKey: string;
  inputSchema: string;
  outputSchema: string | null;
  status: ToolStatus;
  requiresApproval: boolean;
  maxExecutionSeconds: number;
  maxOutputCharacters: number;
  version: number;
  createdBy: string;
  updatedBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ToolCreateRequest {
  toolKey: string;
  name: string;
  description?: string | null;
  executorKey: string;
  inputSchema: string;
  outputSchema?: string | null;
  requiresApproval: boolean;
  maxExecutionSeconds: number;
  maxOutputCharacters: number;
}

export interface ToolUpdateRequest {
  version: number;
  name: string;
  description?: string | null;
  executorKey?: string;
  inputSchema: string;
  outputSchema?: string | null;
  requiresApproval: boolean;
  maxExecutionSeconds: number;
  maxOutputCharacters: number;
}

export interface ToolListParams {
  search?: string;
  status?: ToolStatus;
  type?: ToolType;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ExecutorKeysResponse {
  executorKeys: string[];
}

export interface AgentToolAssignment {
  id: string;
  agentId: string;
  toolId: string;
  toolKey: string;
  toolName: string;
  toolStatus: ToolStatus;
  enabled: boolean;
  version: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentToolAssignRequest {
  toolId: string;
}

export interface ExecutionToolCall {
  id: string;
  executionId: string;
  agentId: string;
  toolId: string;
  toolKey: string;
  runtimeCallId: string;
  sequenceNumber: number;
  status: ToolCallStatus;
  inputPayload: string;
  outputPayload: string | null;
  errorCode: string | null;
  requestedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number | null;
  approvedBy: string | null;
  approvedAt: string | null;
  createdBy: string;
}

export interface ToolCallApproveRequest {
  version: number;
}

export interface ToolCallRejectRequest {
  version: number;
  reasonCode: string;
}

export interface ExecutionContinueResponse {
  executionId: string;
  readyToContinue: boolean;
  message: string;
}
