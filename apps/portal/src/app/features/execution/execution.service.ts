import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  AgentExecuteRequest,
  AgentExecuteResponse,
  Execution,
  ExecutionListParams,
} from './execution.models';

@Injectable({ providedIn: 'root' })
export class ExecutionService {
  private readonly api = inject(ApiClient);

  execute(
    projectId: string,
    agentId: string,
    body: AgentExecuteRequest,
  ): Observable<AgentExecuteResponse> {
    return this.api.post<AgentExecuteResponse>(
      `/api/projects/${projectId}/agents/${agentId}/execute`,
      body,
    );
  }

  list(projectId: string, params: ExecutionListParams = {}): Observable<PageResponse<Execution>> {
    return this.api.get<PageResponse<Execution>>(
      `/api/projects/${projectId}/executions`,
      this.toQuery(params),
    );
  }

  get(projectId: string, executionId: string): Observable<Execution> {
    return this.api.get<Execution>(`/api/projects/${projectId}/executions/${executionId}`);
  }

  cancel(projectId: string, executionId: string): Observable<Execution> {
    return this.api.post<Execution>(`/api/projects/${projectId}/executions/${executionId}/cancel`, {});
  }

  private toQuery(params: ExecutionListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.agentId) {
      query['agentId'] = params.agentId;
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
