import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse, Project, ProjectRequest } from '../../core/models/catalog';

export interface ListParams {
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly api = inject(ApiClient);

  list(params: ListParams = {}): Observable<PageResponse<Project>> {
    return this.api.get<PageResponse<Project>>('/api/projects', this.toQuery(params));
  }

  get(id: string): Observable<Project> {
    return this.api.get<Project>(`/api/projects/${id}`);
  }

  create(body: ProjectRequest): Observable<Project> {
    return this.api.post<Project>('/api/projects', body);
  }

  update(id: string, body: ProjectRequest): Observable<Project> {
    return this.api.put<Project>(`/api/projects/${id}`, body);
  }

  remove(id: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${id}`);
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
