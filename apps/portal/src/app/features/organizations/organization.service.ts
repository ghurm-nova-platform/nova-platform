import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { Organization, OrganizationRequest, PageResponse } from '../../core/models/catalog';

export interface ListParams {
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly api = inject(ApiClient);

  list(params: ListParams = {}): Observable<PageResponse<Organization>> {
    return this.api.get<PageResponse<Organization>>('/api/organizations', this.toQuery(params));
  }

  get(id: string): Observable<Organization> {
    return this.api.get<Organization>(`/api/organizations/${id}`);
  }

  create(body: OrganizationRequest): Observable<Organization> {
    return this.api.post<Organization>('/api/organizations', body);
  }

  update(id: string, body: OrganizationRequest): Observable<Organization> {
    return this.api.put<Organization>(`/api/organizations/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.api.delete<void>(`/api/organizations/${id}`);
  }

  private toQuery(params: ListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
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
