import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  AgentModelAssignRequest,
  AgentModelAssignment,
  AgentModelUpdateRequest,
  AiModel,
  AiModelCreateRequest,
  AiModelListParams,
  AiModelUpdateRequest,
  ProjectModelAssignRequest,
  ProjectModelAssignment,
  ProjectModelUpdateRequest,
} from './model-gateway.models';

@Injectable({ providedIn: 'root' })
export class AiModelService {
  private readonly api = inject(ApiClient);

  createModel(providerId: string, body: AiModelCreateRequest): Observable<AiModel> {
    return this.api.post<AiModel>(`/api/model-providers/${providerId}/models`, body);
  }

  listModels(providerId: string, params: AiModelListParams = {}): Observable<PageResponse<AiModel>> {
    return this.api.get<PageResponse<AiModel>>(
      `/api/model-providers/${providerId}/models`,
      this.toModelQuery(params),
    );
  }

  getModel(providerId: string, modelId: string): Observable<AiModel> {
    return this.api.get<AiModel>(`/api/model-providers/${providerId}/models/${modelId}`);
  }

  updateModel(providerId: string, modelId: string, body: AiModelUpdateRequest): Observable<AiModel> {
    return this.api.put<AiModel>(`/api/model-providers/${providerId}/models/${modelId}`, body);
  }

  activateModel(providerId: string, modelId: string): Observable<AiModel> {
    return this.api.post<AiModel>(`/api/model-providers/${providerId}/models/${modelId}/activate`, {});
  }

  disableModel(providerId: string, modelId: string): Observable<AiModel> {
    return this.api.post<AiModel>(`/api/model-providers/${providerId}/models/${modelId}/disable`, {});
  }

  archiveModel(providerId: string, modelId: string): Observable<void> {
    return this.api.delete<void>(`/api/model-providers/${providerId}/models/${modelId}`);
  }

  listProjectModels(projectId: string): Observable<ProjectModelAssignment[]> {
    return this.api.get<ProjectModelAssignment[]>(`/api/projects/${projectId}/models`);
  }

  assignProjectModel(projectId: string, body: ProjectModelAssignRequest): Observable<ProjectModelAssignment> {
    return this.api.post<ProjectModelAssignment>(`/api/projects/${projectId}/models`, body);
  }

  updateProjectModel(
    projectId: string,
    modelId: string,
    body: ProjectModelUpdateRequest,
  ): Observable<ProjectModelAssignment> {
    return this.api.put<ProjectModelAssignment>(`/api/projects/${projectId}/models/${modelId}`, body);
  }

  unassignProjectModel(projectId: string, modelId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/models/${modelId}`);
  }

  listAgentModels(projectId: string, agentId: string): Observable<AgentModelAssignment[]> {
    return this.api.get<AgentModelAssignment[]>(`/api/projects/${projectId}/agents/${agentId}/models`);
  }

  assignAgentModel(
    projectId: string,
    agentId: string,
    body: AgentModelAssignRequest,
  ): Observable<AgentModelAssignment> {
    return this.api.post<AgentModelAssignment>(
      `/api/projects/${projectId}/agents/${agentId}/models`,
      body,
    );
  }

  updateAgentModel(
    projectId: string,
    agentId: string,
    modelId: string,
    body: AgentModelUpdateRequest,
  ): Observable<AgentModelAssignment> {
    return this.api.put<AgentModelAssignment>(
      `/api/projects/${projectId}/agents/${agentId}/models/${modelId}`,
      body,
    );
  }

  unassignAgentModel(projectId: string, agentId: string, modelId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/agents/${agentId}/models/${modelId}`);
  }

  private toModelQuery(params: AiModelListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.modelType) {
      query['modelType'] = params.modelType;
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
