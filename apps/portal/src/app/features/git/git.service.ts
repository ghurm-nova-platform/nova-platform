import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { GitOperation } from './git.models';

@Injectable({ providedIn: 'root' })
export class GitService {
  private readonly api = inject(ApiClient);

  run(taskId: string): Observable<GitOperation> {
    return this.api.post<GitOperation>('/api/git/run', { taskId });
  }

  getLatest(taskId: string): Observable<GitOperation> {
    return this.api.get<GitOperation>(`/api/git/${taskId}`);
  }
}
