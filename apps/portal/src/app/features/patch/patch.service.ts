import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PatchResult } from './patch.models';

@Injectable({ providedIn: 'root' })
export class PatchService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<PatchResult> {
    return this.api.post<PatchResult>('/api/patch/run', { taskId });
  }

  getLatest(taskId: string): Observable<PatchResult> {
    return this.api.get<PatchResult>(`/api/patch/${taskId}`);
  }
}
