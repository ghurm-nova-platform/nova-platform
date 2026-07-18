import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  ExecutionPlan,
  PlanAndCreateResponse,
  PlannerRequest,
  PlannerResponse,
  PlannerTemplate,
} from './planner.models';

@Injectable({ providedIn: 'root' })
export class PlannerService {
  private readonly api = inject(ApiClient);

  plan(body: PlannerRequest): Observable<PlannerResponse> {
    return this.api.post<PlannerResponse>('/api/planner/plan', body);
  }

  planAndCreate(body: PlannerRequest): Observable<PlanAndCreateResponse> {
    return this.api.post<PlanAndCreateResponse>('/api/planner/plan-and-create', body);
  }

  importPlan(projectId: string, runName: string, plan: ExecutionPlan): Observable<{ id: string }> {
    return this.api.post<{ id: string }>('/api/planner/import', { projectId, runName, plan });
  }

  listTemplates(projectId: string): Observable<PlannerTemplate[]> {
    return this.api.get<PlannerTemplate[]>('/api/planner/templates', { projectId });
  }
}
