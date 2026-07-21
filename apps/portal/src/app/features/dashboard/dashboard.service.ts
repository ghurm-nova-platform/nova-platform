import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  DashboardConfigResponse,
  DashboardExportFormat,
  DashboardExportSection,
  DashboardSnapshot,
} from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiClient);

  getSnapshot(projectId?: string): Observable<DashboardSnapshot> {
    return this.api.get<DashboardSnapshot>(`/api/dashboard${this.projectQuery(projectId)}`);
  }

  getConfig(): Observable<DashboardConfigResponse> {
    return this.api.get<DashboardConfigResponse>('/api/dashboard/config');
  }

  refresh(projectId?: string): Observable<{ refreshedAt: string; cacheExpiresAt: string }> {
    return this.api.post(`/api/dashboard/refresh${this.projectQuery(projectId)}`, {});
  }

  exportUrl(format: DashboardExportFormat, section: DashboardExportSection, projectId?: string): string {
    const params = new URLSearchParams({ format, section });
    if (projectId) {
      params.set('projectId', projectId);
    }
    return `/api/dashboard/export?${params.toString()}`;
  }

  private projectQuery(projectId?: string): string {
    return projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
  }
}
