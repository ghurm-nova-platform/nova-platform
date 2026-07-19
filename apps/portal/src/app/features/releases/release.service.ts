import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CreateReleaseRequest, Release } from './release.models';

@Injectable({ providedIn: 'root' })
export class ReleaseService {
  private readonly api = inject(ApiClient);

  create(request: CreateReleaseRequest): Observable<Release> {
    return this.api.post<Release>('/api/releases/create', request);
  }

  prepare(id: string): Observable<Release> {
    return this.api.post<Release>(`/api/releases/${id}/prepare`, {});
  }

  publish(id: string): Observable<Release> {
    return this.api.post<Release>(`/api/releases/${id}/publish`, {});
  }

  list(projectId?: string): Observable<Release[]> {
    const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
    return this.api.get<Release[]>(`/api/releases${query}`);
  }

  get(id: string): Observable<Release> {
    return this.api.get<Release>(`/api/releases/${id}`);
  }

  history(id: string): Observable<Release> {
    return this.api.get<Release>(`/api/releases/${id}/history`);
  }
}
