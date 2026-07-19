import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { RepairOperation } from './repair.models';

@Injectable({ providedIn: 'root' })
export class RepairService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<RepairOperation> {
    return this.api.post<RepairOperation>('/api/repair/run', { taskId });
  }

  getLatest(taskId: string): Observable<RepairOperation> {
    return this.api.get<RepairOperation>(`/api/repair/${taskId}`);
  }

  getHistory(taskId: string): Observable<RepairOperation[]> {
    return this.api.get<RepairOperation[]>(`/api/repair/${taskId}/history`);
  }
}
