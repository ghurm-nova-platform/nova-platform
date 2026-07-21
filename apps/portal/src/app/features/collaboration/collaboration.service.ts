import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  AssignTaskRequest,
  CollaborationConfigResponse,
  CreateSessionRequest,
  MessageView,
  ParticipantView,
  RecordDecisionRequest,
  SendMessageRequest,
  SessionDetail,
  SessionSummary,
  TimelineEventView,
} from './collaboration.models';

@Injectable({ providedIn: 'root' })
export class CollaborationService {
  private readonly api = inject(ApiClient);

  list(projectId?: string): Observable<SessionSummary[]> {
    return this.api.get<SessionSummary[]>(`/api/collaboration${this.projectQuery(projectId)}`);
  }

  getConfig(): Observable<CollaborationConfigResponse> {
    return this.api.get<CollaborationConfigResponse>('/api/collaboration/config');
  }

  get(id: string): Observable<SessionDetail> {
    return this.api.get<SessionDetail>(`/api/collaboration/${encodeURIComponent(id)}`);
  }

  timeline(id: string): Observable<TimelineEventView[]> {
    return this.api.get<TimelineEventView[]>(`/api/collaboration/${encodeURIComponent(id)}/timeline`);
  }

  participants(id: string): Observable<ParticipantView[]> {
    return this.api.get<ParticipantView[]>(`/api/collaboration/${encodeURIComponent(id)}/participants`);
  }

  messages(id: string): Observable<MessageView[]> {
    return this.api.get<MessageView[]>(`/api/collaboration/${encodeURIComponent(id)}/messages`);
  }

  create(request: CreateSessionRequest): Observable<SessionDetail> {
    return this.api.post<SessionDetail>('/api/collaboration', request);
  }

  assign(id: string, request: AssignTaskRequest): Observable<SessionDetail> {
    return this.api.post<SessionDetail>(`/api/collaboration/${encodeURIComponent(id)}/assign`, request);
  }

  sendMessage(id: string, request: SendMessageRequest): Observable<SessionDetail> {
    return this.api.post<SessionDetail>(`/api/collaboration/${encodeURIComponent(id)}/message`, request);
  }

  recordDecision(id: string, request: RecordDecisionRequest): Observable<SessionDetail> {
    return this.api.post<SessionDetail>(`/api/collaboration/${encodeURIComponent(id)}/decision`, request);
  }

  pause(id: string): Observable<SessionDetail> {
    return this.api.post<SessionDetail>(`/api/collaboration/${encodeURIComponent(id)}/pause`, {});
  }

  resume(id: string): Observable<SessionDetail> {
    return this.api.post<SessionDetail>(`/api/collaboration/${encodeURIComponent(id)}/resume`, {});
  }

  cancel(id: string): Observable<SessionDetail> {
    return this.api.post<SessionDetail>(`/api/collaboration/${encodeURIComponent(id)}/cancel`, {});
  }

  private projectQuery(projectId?: string): string {
    return projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
  }
}
