import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  AuditEvent,
  AuditHistoryResponse,
  AuditSearchParams,
  AuditSearchResponse,
} from './audit.models';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly api = inject(ApiClient);

  list(params: AuditSearchParams = {}): Observable<AuditSearchResponse> {
    const query = this.toQuery(params);
    return this.api.get<AuditSearchResponse>(`/api/audit${query}`);
  }

  get(id: string): Observable<AuditEvent> {
    return this.api.get<AuditEvent>(`/api/audit/${encodeURIComponent(id)}`);
  }

  history(entityType: string, entityId: string): Observable<AuditHistoryResponse> {
    return this.api.get<AuditHistoryResponse>(
      `/api/audit/history?entityType=${encodeURIComponent(entityType)}&entityId=${encodeURIComponent(entityId)}`,
    );
  }

  search(params: AuditSearchParams): Observable<AuditSearchResponse> {
    const query = this.toQuery(params);
    return this.api.get<AuditSearchResponse>(`/api/audit/search${query}`);
  }

  private toQuery(params: AuditSearchParams): string {
    const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== '');
    if (entries.length === 0) {
      return '';
    }
    const query = entries
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
      .join('&');
    return `?${query}`;
  }
}
