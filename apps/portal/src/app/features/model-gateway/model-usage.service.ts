import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { ModelUsageListParams, ModelUsageResponse } from './model-gateway.models';

@Injectable({ providedIn: 'root' })
export class ModelUsageService {
  private readonly api = inject(ApiClient);

  getUsage(projectId: string, params: ModelUsageListParams = {}): Observable<ModelUsageResponse> {
    return this.api.get<ModelUsageResponse>(`/api/projects/${projectId}/model-usage`, this.toUsageQuery(params));
  }

  private toUsageQuery(params: ModelUsageListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.from) {
      query['from'] = params.from;
    }
    if (params.to) {
      query['to'] = params.to;
    }
    if (params.providerId) {
      query['providerId'] = params.providerId;
    }
    if (params.modelId) {
      query['modelId'] = params.modelId;
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
