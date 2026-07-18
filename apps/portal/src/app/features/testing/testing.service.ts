import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { TestingResult } from './testing.models';

@Injectable({ providedIn: 'root' })
export class TestingService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<TestingResult> {
    return this.api.post<TestingResult>('/api/testing/run', { taskId });
  }

  getLatest(taskId: string): Observable<TestingResult> {
    return this.api.get<TestingResult>(`/api/testing/${taskId}`);
  }
}
