import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CreateRollbackRequest, Rollback } from './rollback.models';

@Injectable({ providedIn: 'root' })
export class RollbackService {
  private readonly api = inject(ApiClient);

  create(request: CreateRollbackRequest): Observable<Rollback> {
    return this.api.post<Rollback>('/api/rollbacks/create', request);
  }

  validate(id: string): Observable<Rollback> {
    return this.api.post<Rollback>(`/api/rollbacks/${id}/validate`, {});
  }

  list(projectId?: string): Observable<Rollback[]> {
    const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
    return this.api.get<Rollback[]>(`/api/rollbacks${query}`);
  }

  get(id: string): Observable<Rollback> {
    return this.api.get<Rollback>(`/api/rollbacks/${id}`);
  }

  history(id: string): Observable<Rollback> {
    return this.api.get<Rollback>(`/api/rollbacks/${id}/history`);
  }
}
