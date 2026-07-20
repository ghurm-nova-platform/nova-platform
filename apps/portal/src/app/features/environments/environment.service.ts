import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  CreateEnvironmentRequest,
  ManagedEnvironment,
  UpdateEnvironmentRequest,
} from './environment.models';

@Injectable({ providedIn: 'root' })
export class EnvironmentService {
  private readonly api = inject(ApiClient);

  create(request: CreateEnvironmentRequest): Observable<ManagedEnvironment> {
    return this.api.post<ManagedEnvironment>('/api/environments', request);
  }

  update(id: string, request: UpdateEnvironmentRequest): Observable<ManagedEnvironment> {
    return this.api.put<ManagedEnvironment>(`/api/environments/${id}`, request);
  }

  enable(id: string): Observable<ManagedEnvironment> {
    return this.api.post<ManagedEnvironment>(`/api/environments/${id}/enable`, {});
  }

  disable(id: string): Observable<ManagedEnvironment> {
    return this.api.post<ManagedEnvironment>(`/api/environments/${id}/disable`, {});
  }

  archive(id: string): Observable<ManagedEnvironment> {
    return this.api.post<ManagedEnvironment>(`/api/environments/${id}/archive`, {});
  }

  list(projectId: string): Observable<ManagedEnvironment[]> {
    return this.api.get<ManagedEnvironment[]>(
      `/api/environments?projectId=${encodeURIComponent(projectId)}`,
    );
  }

  get(id: string): Observable<ManagedEnvironment> {
    return this.api.get<ManagedEnvironment>(`/api/environments/${id}`);
  }

  history(id: string): Observable<ManagedEnvironment> {
    return this.api.get<ManagedEnvironment>(`/api/environments/${id}/history`);
  }
}
