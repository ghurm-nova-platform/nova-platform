import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CiObservationOperation } from './ci.models';

@Injectable({ providedIn: 'root' })
export class CiService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<CiObservationOperation> {
    return this.api.post<CiObservationOperation>('/api/ci/run', { taskId });
  }

  getLatest(taskId: string): Observable<CiObservationOperation> {
    return this.api.get<CiObservationOperation>(`/api/ci/${taskId}`);
  }

  getHistory(taskId: string): Observable<CiObservationOperation[]> {
    return this.api.get<CiObservationOperation[]>(`/api/ci/${taskId}/history`);
  }
}
