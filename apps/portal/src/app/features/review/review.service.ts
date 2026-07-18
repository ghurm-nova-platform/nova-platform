import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { ReviewResult } from './review.models';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<ReviewResult> {
    return this.api.post<ReviewResult>('/api/review/run', { taskId });
  }

  getLatest(taskId: string): Observable<ReviewResult> {
    return this.api.get<ReviewResult>(`/api/review/${taskId}`);
  }
}
