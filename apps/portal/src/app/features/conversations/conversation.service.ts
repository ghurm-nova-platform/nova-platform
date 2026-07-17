import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  Conversation,
  ConversationCreateRequest,
  ConversationListParams,
  ConversationMessage,
  ConversationMessageCreateRequest,
  ConversationMessageListParams,
  ConversationUpdateRequest,
} from './conversation.models';

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly api = inject(ApiClient);

  list(projectId: string, params: ConversationListParams = {}): Observable<PageResponse<Conversation>> {
    return this.api.get<PageResponse<Conversation>>(
      `/api/projects/${projectId}/conversations`,
      this.toConversationQuery(params),
    );
  }

  get(projectId: string, conversationId: string): Observable<Conversation> {
    return this.api.get<Conversation>(`/api/projects/${projectId}/conversations/${conversationId}`);
  }

  create(projectId: string, body: ConversationCreateRequest): Observable<Conversation> {
    return this.api.post<Conversation>(`/api/projects/${projectId}/conversations`, body);
  }

  update(
    projectId: string,
    conversationId: string,
    body: ConversationUpdateRequest,
  ): Observable<Conversation> {
    return this.api.put<Conversation>(
      `/api/projects/${projectId}/conversations/${conversationId}`,
      body,
    );
  }

  archive(projectId: string, conversationId: string): Observable<Conversation> {
    return this.api.delete<Conversation>(`/api/projects/${projectId}/conversations/${conversationId}`);
  }

  restore(projectId: string, conversationId: string): Observable<Conversation> {
    return this.api.post<Conversation>(
      `/api/projects/${projectId}/conversations/${conversationId}/restore`,
      {},
    );
  }

  listMessages(
    projectId: string,
    conversationId: string,
    params: ConversationMessageListParams = {},
  ): Observable<PageResponse<ConversationMessage>> {
    return this.api.get<PageResponse<ConversationMessage>>(
      `/api/projects/${projectId}/conversations/${conversationId}/messages`,
      this.toMessageQuery(params),
    );
  }

  addMessage(
    projectId: string,
    conversationId: string,
    body: ConversationMessageCreateRequest,
  ): Observable<ConversationMessage> {
    return this.api.post<ConversationMessage>(
      `/api/projects/${projectId}/conversations/${conversationId}/messages`,
      body,
    );
  }

  private toConversationQuery(params: ConversationListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.agentId) {
      query['agentId'] = params.agentId;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.page !== undefined) {
      query['page'] = String(params.page);
    }
    if (params.size !== undefined) {
      query['size'] = String(params.size);
    }
    if (params.sort) {
      query['sort'] = params.sort;
    }
    return query;
  }

  private toMessageQuery(params: ConversationMessageListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.page !== undefined) {
      query['page'] = String(params.page);
    }
    if (params.size !== undefined) {
      query['size'] = String(params.size);
    }
    if (params.sort) {
      query['sort'] = params.sort;
    }
    return query;
  }
}
