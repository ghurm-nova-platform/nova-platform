import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  AgentToolAssignRequest,
  AgentToolAssignment,
  ExecutionContinueResponse,
  ExecutionToolCall,
  ExecutorKeysResponse,
  ToolCallApproveRequest,
  ToolCallRejectRequest,
  ToolCreateRequest,
  ToolDefinition,
  ToolListParams,
  ToolUpdateRequest,
} from './tool.models';

@Injectable({ providedIn: 'root' })
export class ToolService {
  private readonly api = inject(ApiClient);

  createTool(projectId: string, body: ToolCreateRequest): Observable<ToolDefinition> {
    return this.api.post<ToolDefinition>(`/api/projects/${projectId}/tools`, body);
  }

  listTools(projectId: string, params: ToolListParams = {}): Observable<PageResponse<ToolDefinition>> {
    return this.api.get<PageResponse<ToolDefinition>>(
      `/api/projects/${projectId}/tools`,
      this.toToolQuery(params),
    );
  }

  getTool(projectId: string, toolId: string): Observable<ToolDefinition> {
    return this.api.get<ToolDefinition>(`/api/projects/${projectId}/tools/${toolId}`);
  }

  updateTool(projectId: string, toolId: string, body: ToolUpdateRequest): Observable<ToolDefinition> {
    return this.api.put<ToolDefinition>(`/api/projects/${projectId}/tools/${toolId}`, body);
  }

  activateTool(projectId: string, toolId: string): Observable<ToolDefinition> {
    return this.api.post<ToolDefinition>(`/api/projects/${projectId}/tools/${toolId}/activate`, {});
  }

  archiveTool(projectId: string, toolId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/tools/${toolId}`);
  }

  listExecutors(projectId: string): Observable<ExecutorKeysResponse> {
    return this.api.get<ExecutorKeysResponse>(`/api/projects/${projectId}/tools/executors`);
  }

  listAgentTools(projectId: string, agentId: string): Observable<AgentToolAssignment[]> {
    return this.api.get<AgentToolAssignment[]>(`/api/projects/${projectId}/agents/${agentId}/tools`);
  }

  assignTool(
    projectId: string,
    agentId: string,
    body: AgentToolAssignRequest,
  ): Observable<AgentToolAssignment> {
    return this.api.post<AgentToolAssignment>(`/api/projects/${projectId}/agents/${agentId}/tools`, body);
  }

  unassignTool(projectId: string, agentId: string, toolId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/agents/${agentId}/tools/${toolId}`);
  }

  listExecutionToolCalls(projectId: string, executionId: string): Observable<ExecutionToolCall[]> {
    return this.api.get<ExecutionToolCall[]>(
      `/api/projects/${projectId}/executions/${executionId}/tool-calls`,
    );
  }

  getExecutionToolCall(
    projectId: string,
    executionId: string,
    toolCallId: string,
  ): Observable<ExecutionToolCall> {
    return this.api.get<ExecutionToolCall>(
      `/api/projects/${projectId}/executions/${executionId}/tool-calls/${toolCallId}`,
    );
  }

  approveToolCall(
    projectId: string,
    executionId: string,
    toolCallId: string,
    body: ToolCallApproveRequest,
  ): Observable<ExecutionToolCall> {
    return this.api.post<ExecutionToolCall>(
      `/api/projects/${projectId}/executions/${executionId}/tool-calls/${toolCallId}/approve`,
      body,
    );
  }

  rejectToolCall(
    projectId: string,
    executionId: string,
    toolCallId: string,
    body: ToolCallRejectRequest,
  ): Observable<ExecutionToolCall> {
    return this.api.post<ExecutionToolCall>(
      `/api/projects/${projectId}/executions/${executionId}/tool-calls/${toolCallId}/reject`,
      body,
    );
  }

  continueExecution(projectId: string, executionId: string): Observable<ExecutionContinueResponse> {
    return this.api.post<ExecutionContinueResponse>(
      `/api/projects/${projectId}/executions/${executionId}/continue`,
      {},
    );
  }

  private toToolQuery(params: ToolListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.type) {
      query['type'] = params.type;
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
