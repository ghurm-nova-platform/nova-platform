import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CreatePolicyRequest, EvaluatePolicyRequest, Policy } from './policy.models';

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private readonly api = inject(ApiClient);

  create(request: CreatePolicyRequest): Observable<Policy> {
    return this.api.post<Policy>('/api/policies', request);
  }

  evaluate(id: string, request: EvaluatePolicyRequest): Observable<Policy> {
    return this.api.post<Policy>(`/api/policies/${id}/evaluate`, request);
  }

  enable(id: string): Observable<Policy> {
    return this.api.post<Policy>(`/api/policies/${id}/enable`, {});
  }

  disable(id: string): Observable<Policy> {
    return this.api.post<Policy>(`/api/policies/${id}/disable`, {});
  }

  list(projectId?: string): Observable<Policy[]> {
    const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
    return this.api.get<Policy[]>(`/api/policies${query}`);
  }

  get(id: string): Observable<Policy> {
    return this.api.get<Policy>(`/api/policies/${id}`);
  }

  history(id: string): Observable<Policy> {
    return this.api.get<Policy>(`/api/policies/${id}/history`);
  }
}
