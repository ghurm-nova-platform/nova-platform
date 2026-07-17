export type ConversationStatus = 'ACTIVE' | 'ARCHIVED';

export type ConversationMessageRole = 'SYSTEM' | 'USER' | 'ASSISTANT' | 'TOOL';

export const CONVERSATION_STATUSES: ConversationStatus[] = ['ACTIVE', 'ARCHIVED'];

export interface Conversation {
  id: string;
  projectId: string;
  agentId: string;
  title: string;
  status: ConversationStatus;
  messageCount: number;
  lastMessageAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ConversationCreateRequest {
  agentId: string;
  title?: string | null;
}

export interface ConversationUpdateRequest {
  title: string;
  version: number;
}

export interface ConversationMessage {
  id: string;
  role: ConversationMessageRole;
  content: string;
  sequenceNumber: number;
  executionId: string | null;
  createdAt: string;
}

export interface ConversationMessageCreateRequest {
  content: string;
}

export interface ConversationListParams {
  agentId?: string;
  status?: ConversationStatus;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ConversationMessageListParams {
  page?: number;
  size?: number;
  sort?: string;
}
