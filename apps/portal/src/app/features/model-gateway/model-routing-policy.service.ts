import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  ModelRoutingPolicy,
  ModelRoutingPolicyCreateRequest,
  ModelRoutingPolicyListParams,
  ModelRoutingPolicyUpdateRequest,
} from './model-gateway.models';

@Injectable({ providedIn: 'root' })
export class ModelRoutingPolicyService {
  private readonly api = inject(ApiClient);

  createPolicy(projectId: string, body: ModelRoutingPolicyCreateRequest): Observable<ModelRoutingPolicy> {
    return this.api.post<ModelRoutingPolicy>(`/api/projects/${projectId}/model-routing-policies`, body);
  }

  listPolicies(
    projectId: string,
    params: ModelRoutingPolicyListParams = {},
  ): Observable<PageResponse<ModelRoutingPolicy>> {
    return this.api.get<PageResponse<ModelRoutingPolicy>>(
      `/api/projects/${projectId}/model-routing-policies`,
      this.toPolicyQuery(params),
    );
  }

  getPolicy(projectId: string, policyId: string): Observable<ModelRoutingPolicy> {
    return this.api.get<ModelRoutingPolicy>(
      `/api/projects/${projectId}/model-routing-policies/${policyId}`,
    );
  }

  updatePolicy(
    projectId: string,
    policyId: string,
    body: ModelRoutingPolicyUpdateRequest,
  ): Observable<ModelRoutingPolicy> {
    return this.api.put<ModelRoutingPolicy>(
      `/api/projects/${projectId}/model-routing-policies/${policyId}`,
      body,
    );
  }

  activatePolicy(projectId: string, policyId: string): Observable<ModelRoutingPolicy> {
    return this.api.post<ModelRoutingPolicy>(
      `/api/projects/${projectId}/model-routing-policies/${policyId}/activate`,
      {},
    );
  }

  archivePolicy(projectId: string, policyId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/model-routing-policies/${policyId}`);
  }

  private toPolicyQuery(params: ModelRoutingPolicyListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.agentId) {
      query['agentId'] = params.agentId;
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
