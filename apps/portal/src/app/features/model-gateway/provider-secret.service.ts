import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  ProviderSecret,
  ProviderSecretCreateRequest,
  ProviderSecretListParams,
  ProviderSecretRotateRequest,
} from './model-gateway.models';

@Injectable({ providedIn: 'root' })
export class ProviderSecretService {
  private readonly api = inject(ApiClient);

  listSecrets(params: ProviderSecretListParams = {}): Observable<PageResponse<ProviderSecret>> {
    return this.api.get<PageResponse<ProviderSecret>>('/api/provider-secrets', this.toQuery(params));
  }

  getSecret(secretId: string): Observable<ProviderSecret> {
    return this.api.get<ProviderSecret>(`/api/provider-secrets/${secretId}`);
  }

  createSecret(body: ProviderSecretCreateRequest): Observable<ProviderSecret> {
    return this.api.post<ProviderSecret>('/api/provider-secrets', body);
  }

  rotateSecret(secretId: string, body: ProviderSecretRotateRequest): Observable<ProviderSecret> {
    return this.api.post<ProviderSecret>(`/api/provider-secrets/${secretId}/rotate`, body);
  }

  revokeSecret(secretId: string): Observable<ProviderSecret> {
    return this.api.post<ProviderSecret>(`/api/provider-secrets/${secretId}/revoke`, {});
  }

  private toQuery(params: ProviderSecretListParams): Record<string, string> {
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
