import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import {
  FindingView,
  KnowledgeReferenceView,
  PrReviewConfigResponse,
  RecommendationView,
  ReviewRunDetail,
  ReviewRunSummary,
  RiskScoreView,
  RunRequest,
} from './pr-review.models';

@Injectable({ providedIn: 'root' })
export class PrReviewService {
  private readonly api = inject(ApiClient);
  private readonly http = inject(HttpClient);
  private readonly runtimeConfig = inject(RuntimeConfigService);

  getConfig(): Observable<PrReviewConfigResponse> {
    return this.api.get<PrReviewConfigResponse>('/api/pr-review/config');
  }

  list(projectId?: string): Observable<ReviewRunSummary[]> {
    return this.api.get<ReviewRunSummary[]>(`/api/pr-review${this.projectQuery(projectId)}`);
  }

  history(projectId?: string): Observable<ReviewRunSummary[]> {
    return this.api.get<ReviewRunSummary[]>(`/api/pr-review/history${this.projectQuery(projectId)}`);
  }

  get(id: string): Observable<ReviewRunDetail> {
    return this.api.get<ReviewRunDetail>(`/api/pr-review/${encodeURIComponent(id)}`);
  }

  findings(id: string): Observable<FindingView[]> {
    return this.api.get<FindingView[]>(`/api/pr-review/${encodeURIComponent(id)}/findings`);
  }

  recommendations(id: string): Observable<RecommendationView[]> {
    return this.api.get<RecommendationView[]>(
      `/api/pr-review/${encodeURIComponent(id)}/recommendations`,
    );
  }

  riskScore(id: string): Observable<RiskScoreView> {
    return this.api.get<RiskScoreView>(`/api/pr-review/${encodeURIComponent(id)}/risk-score`);
  }

  knowledge(id: string): Observable<KnowledgeReferenceView[]> {
    return this.api.get<KnowledgeReferenceView[]>(
      `/api/pr-review/${encodeURIComponent(id)}/knowledge`,
    );
  }

  run(request: RunRequest): Observable<ReviewRunDetail> {
    return this.api.post<ReviewRunDetail>('/api/pr-review/run', request);
  }

  rerun(id: string): Observable<ReviewRunDetail> {
    return this.api.post<ReviewRunDetail>(`/api/pr-review/${encodeURIComponent(id)}/rerun`, {});
  }

  export(id: string, format: string): Observable<Blob> {
    const base = this.runtimeConfig.platformApiUrl().replace(/\/$/, '');
    return this.http.get(`${base}/api/pr-review/${encodeURIComponent(id)}/export`, {
      params: { format },
      responseType: 'blob',
    });
  }

  private projectQuery(projectId?: string): string {
    return projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
  }
}
