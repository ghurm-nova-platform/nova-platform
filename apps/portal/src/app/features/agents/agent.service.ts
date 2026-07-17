import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  Agent,
  AgentCreateRequest,
  AgentStatus,
  AgentStatusRequest,
  AgentUpdateRequest,
} from './agent.models';

export interface AgentListParams {
  search?: string;
  status?: AgentStatus;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class AgentService {
  private readonly api = inject(ApiClient);

  list(projectId: string, params: AgentListParams = {}): Observable<PageResponse<Agent>> {
    return this.api.get<PageResponse<Agent>>(
      `/api/projects/${projectId}/agents`,
      this.toQuery(params),
    );
  }

  get(projectId: string, agentId: string): Observable<Agent> {
    return this.api.get<Agent>(`/api/projects/${projectId}/agents/${agentId}`);
  }

  create(projectId: string, body: AgentCreateRequest): Observable<Agent> {
    return this.api.post<Agent>(`/api/projects/${projectId}/agents`, body);
  }

  update(projectId: string, agentId: string, body: AgentUpdateRequest): Observable<Agent> {
    return this.api.put<Agent>(`/api/projects/${projectId}/agents/${agentId}`, body);
  }

  updateStatus(
    projectId: string,
    agentId: string,
    body: AgentStatusRequest,
  ): Observable<Agent> {
    return this.api.patch<Agent>(`/api/projects/${projectId}/agents/${agentId}/status`, body);
  }

  archive(projectId: string, agentId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/agents/${agentId}`);
  }

  private toQuery(params: AgentListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
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
}
