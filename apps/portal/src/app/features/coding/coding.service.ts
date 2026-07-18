import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CodeGenerationRequest, CodingResult, GeneratedArtifact } from './coding.models';

@Injectable({ providedIn: 'root' })
export class CodingService {
  private readonly api = inject(ApiClient);

  generate(body: CodeGenerationRequest): Observable<CodingResult> {
    return this.api.post<CodingResult>('/api/coding/generate', body);
  }

  listArtifacts(taskId: string): Observable<GeneratedArtifact[]> {
    return this.api.get<GeneratedArtifact[]>(`/api/coding/artifacts/${taskId}`);
  }
}
