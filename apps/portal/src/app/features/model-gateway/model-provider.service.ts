import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  AdapterKeysResponse,
  ModelProvider,
  ModelProviderCreateRequest,
  ModelProviderListParams,
  ModelProviderUpdateRequest,
} from './model-gateway.models';

@Injectable({ providedIn: 'root' })
export class ModelProviderService {
  private readonly api = inject(ApiClient);

  createProvider(body: ModelProviderCreateRequest): Observable<ModelProvider> {
    return this.api.post<ModelProvider>('/api/model-providers', body);
  }

  listProviders(params: ModelProviderListParams = {}): Observable<PageResponse<ModelProvider>> {
    return this.api.get<PageResponse<ModelProvider>>('/api/model-providers', this.toProviderQuery(params));
  }

  getProvider(providerId: string): Observable<ModelProvider> {
    return this.api.get<ModelProvider>(`/api/model-providers/${providerId}`);
  }

  updateProvider(providerId: string, body: ModelProviderUpdateRequest): Observable<ModelProvider> {
    return this.api.put<ModelProvider>(`/api/model-providers/${providerId}`, body);
  }

  activateProvider(providerId: string): Observable<ModelProvider> {
    return this.api.post<ModelProvider>(`/api/model-providers/${providerId}/activate`, {});
  }

  disableProvider(providerId: string): Observable<ModelProvider> {
    return this.api.post<ModelProvider>(`/api/model-providers/${providerId}/disable`, {});
  }

  archiveProvider(providerId: string): Observable<void> {
    return this.api.delete<void>(`/api/model-providers/${providerId}`);
  }

  listAdapters(): Observable<AdapterKeysResponse> {
    return this.api.get<AdapterKeysResponse>('/api/model-providers/adapters');
  }

  private toProviderQuery(params: ModelProviderListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.providerType) {
      query['providerType'] = params.providerType;
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
