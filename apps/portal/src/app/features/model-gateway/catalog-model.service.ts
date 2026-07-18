import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  CatalogAliasCreateRequest,
  CatalogCapabilityInput,
  CatalogModel,
  CatalogModelAlias,
  CatalogModelCreateRequest,
  CatalogModelListParams,
  CatalogModelUpdateRequest,
  CatalogSyncResult,
} from './model-gateway.models';

@Injectable({ providedIn: 'root' })
export class CatalogModelService {
  private readonly api = inject(ApiClient);

  list(params: CatalogModelListParams = {}): Observable<PageResponse<CatalogModel>> {
    return this.api.get<PageResponse<CatalogModel>>('/api/ai-models', this.toQuery(params));
  }

  get(modelId: string): Observable<CatalogModel> {
    return this.api.get<CatalogModel>(`/api/ai-models/${modelId}`);
  }

  create(body: CatalogModelCreateRequest): Observable<CatalogModel> {
    return this.api.post<CatalogModel>('/api/ai-models', body);
  }

  update(modelId: string, body: CatalogModelUpdateRequest): Observable<CatalogModel> {
    return this.api.put<CatalogModel>(`/api/ai-models/${modelId}`, body);
  }

  activate(modelId: string): Observable<CatalogModel> {
    return this.api.post<CatalogModel>(`/api/ai-models/${modelId}/activate`, {});
  }

  disable(modelId: string): Observable<CatalogModel> {
    return this.api.post<CatalogModel>(`/api/ai-models/${modelId}/disable`, {});
  }

  deprecate(modelId: string): Observable<CatalogModel> {
    return this.api.post<CatalogModel>(`/api/ai-models/${modelId}/deprecate`, {});
  }

  archive(modelId: string): Observable<CatalogModel> {
    return this.api.post<CatalogModel>(`/api/ai-models/${modelId}/archive`, {});
  }

  replaceCapabilities(modelId: string, capabilities: CatalogCapabilityInput[]): Observable<CatalogModel> {
    return this.api.put<CatalogModel>(`/api/ai-models/${modelId}/capabilities`, { capabilities });
  }

  listAliases(modelId: string): Observable<CatalogModelAlias[]> {
    return this.api.get<CatalogModelAlias[]>(`/api/ai-models/${modelId}/aliases`);
  }

  createAlias(modelId: string, body: CatalogAliasCreateRequest): Observable<CatalogModelAlias> {
    return this.api.post<CatalogModelAlias>(`/api/ai-models/${modelId}/aliases`, body);
  }

  deleteAlias(aliasId: string): Observable<void> {
    return this.api.delete<void>(`/api/ai-model-aliases/${aliasId}`);
  }

  syncModels(providerId: string): Observable<CatalogSyncResult> {
    return this.api.post<CatalogSyncResult>(`/api/model-providers/${providerId}/models/sync`, {});
  }

  private toQuery(params: CatalogModelListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.source) {
      query['source'] = params.source;
    }
    if (params.capability) {
      query['capability'] = params.capability;
    }
    if (params.providerId) {
      query['providerId'] = params.providerId;
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
