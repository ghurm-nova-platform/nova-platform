import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { MergeOperation } from './merge.models';

@Injectable({ providedIn: 'root' })
export class MergeService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<MergeOperation> {
    return this.api.post<MergeOperation>('/api/merge/run', { taskId });
  }

  getLatest(taskId: string): Observable<MergeOperation> {
    return this.api.get<MergeOperation>(`/api/merge/${taskId}`);
  }

  getHistory(taskId: string): Observable<MergeOperation[]> {
    return this.api.get<MergeOperation[]>(`/api/merge/${taskId}/history`);
  }
}
