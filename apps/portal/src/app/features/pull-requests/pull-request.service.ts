import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PullRequestOperation } from './pull-request.models';

@Injectable({ providedIn: 'root' })
export class PullRequestService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<PullRequestOperation> {
    return this.api.post<PullRequestOperation>('/api/pull-requests/run', { taskId });
  }

  getLatest(taskId: string): Observable<PullRequestOperation> {
    return this.api.get<PullRequestOperation>(`/api/pull-requests/${taskId}`);
  }

  getHistory(taskId: string): Observable<PullRequestOperation[]> {
    return this.api.get<PullRequestOperation[]>(`/api/pull-requests/${taskId}/history`);
  }
}
