import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  ApprovalDecision,
  ApprovalHumanActionRequest,
  ApprovalRequirement,
} from './approval-gate.models';

@Injectable({ providedIn: 'root' })
export class ApprovalGateService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<ApprovalDecision> {
    return this.api.post<ApprovalDecision>('/api/approval-gate/run', { taskId });
  }

  getLatest(taskId: string): Observable<ApprovalDecision> {
    return this.api.get<ApprovalDecision>(`/api/approval-gate/${taskId}`);
  }

  getHistory(taskId: string): Observable<ApprovalDecision[]> {
    return this.api.get<ApprovalDecision[]>(`/api/approval-gate/${taskId}/history`);
  }

  getRequirements(taskId: string): Observable<ApprovalRequirement[]> {
    return this.api.get<ApprovalRequirement[]>(`/api/approval-gate/${taskId}/requirements`);
  }

  approve(taskId: string, request: ApprovalHumanActionRequest = {}): Observable<ApprovalDecision> {
    return this.api.post<ApprovalDecision>(`/api/approval-gate/${taskId}/approve`, request);
  }

  reject(taskId: string, request: ApprovalHumanActionRequest): Observable<ApprovalDecision> {
    return this.api.post<ApprovalDecision>(`/api/approval-gate/${taskId}/reject`, request);
  }
}
