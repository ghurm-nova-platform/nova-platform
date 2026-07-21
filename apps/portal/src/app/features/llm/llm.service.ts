import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  LlmChatCompletionRequest,
  LlmCompletionResponse,
  LlmConfigEntryView,
  LlmConfigResponse,
  LlmConversationView,
  LlmCreateConversationRequest,
  LlmHealthResponse,
  LlmMessageView,
  LlmMetricsSummaryResponse,
  LlmModelView,
  LlmPromptView,
  LlmProviderStatusView,
  LlmSetConfigRequest,
  LlmTextCompletionRequest,
} from './llm.models';

@Injectable({ providedIn: 'root' })
export class LlmService {
  private readonly api = inject(ApiClient);

  getConfig(): Observable<LlmConfigResponse> {
    return this.api.get<LlmConfigResponse>('/api/llm/config');
  }

  listRuntimeConfig(): Observable<LlmConfigEntryView[]> {
    return this.api.get<LlmConfigEntryView[]>('/api/llm/runtime-config');
  }

  setRuntimeConfig(request: LlmSetConfigRequest): Observable<LlmConfigEntryView> {
    return this.api.post<LlmConfigEntryView>('/api/llm/runtime-config', request);
  }

  getHealth(): Observable<LlmHealthResponse> {
    return this.api.get<LlmHealthResponse>('/api/llm/health');
  }

  getMetrics(): Observable<LlmMetricsSummaryResponse> {
    return this.api.get<LlmMetricsSummaryResponse>('/api/llm/metrics');
  }

  listProviders(): Observable<LlmProviderStatusView[]> {
    return this.api.get<LlmProviderStatusView[]>('/api/llm/providers');
  }

  refreshProviders(): Observable<LlmProviderStatusView[]> {
    return this.api.post<LlmProviderStatusView[]>('/api/llm/providers/health', {});
  }

  listModels(): Observable<LlmModelView[]> {
    return this.api.get<LlmModelView[]>('/api/llm/models');
  }

  getModel(id: string): Observable<LlmModelView> {
    return this.api.get<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}`);
  }

  enableModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/enable`, {});
  }

  disableModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/disable`, {});
  }

  installModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/install`, {});
  }

  downloadModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/download`, {});
  }

  loadModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/load`, {});
  }

  unloadModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/unload`, {});
  }

  startModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/start`, {});
  }

  stopModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/stop`, {});
  }

  restartModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/restart`, {});
  }

  warmupModel(id: string): Observable<LlmModelView> {
    return this.api.post<LlmModelView>(`/api/llm/models/${encodeURIComponent(id)}/warmup`, {});
  }

  chat(request: LlmChatCompletionRequest): Observable<LlmCompletionResponse> {
    return this.api.post<LlmCompletionResponse>('/api/llm/chat', request);
  }

  completions(request: LlmTextCompletionRequest): Observable<LlmCompletionResponse> {
    return this.api.post<LlmCompletionResponse>('/api/llm/completions', request);
  }

  listPrompts(): Observable<LlmPromptView[]> {
    return this.api.get<LlmPromptView[]>('/api/llm/prompts');
  }

  getPrompt(id: string): Observable<LlmPromptView> {
    return this.api.get<LlmPromptView>(`/api/llm/prompts/${encodeURIComponent(id)}`);
  }

  listConversations(): Observable<LlmConversationView[]> {
    return this.api.get<LlmConversationView[]>('/api/llm/conversations');
  }

  createConversation(request: LlmCreateConversationRequest): Observable<LlmConversationView> {
    return this.api.post<LlmConversationView>('/api/llm/conversations', request);
  }

  getConversation(id: string): Observable<LlmConversationView> {
    return this.api.get<LlmConversationView>(`/api/llm/conversations/${encodeURIComponent(id)}`);
  }

  listMessages(conversationId: string): Observable<LlmMessageView[]> {
    return this.api.get<LlmMessageView[]>(
      `/api/llm/conversations/${encodeURIComponent(conversationId)}/messages`,
    );
  }
}
