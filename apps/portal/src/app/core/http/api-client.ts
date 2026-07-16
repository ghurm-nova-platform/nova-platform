import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { RuntimeConfigService } from '../config/runtime-config.service';

export type ApiTarget = 'platform' | 'agent-runtime';

/**
 * Reusable HTTP API client foundation for platform and agent-runtime backends.
 */
@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly runtimeConfig = inject(RuntimeConfigService);

  get<T>(target: ApiTarget, path: string, params?: Record<string, string>): Observable<T> {
    return this.http.get<T>(this.url(target, path), {
      params: this.toParams(params),
    });
  }

  post<T>(target: ApiTarget, path: string, body: unknown): Observable<T> {
    return this.http.post<T>(this.url(target, path), body);
  }

  put<T>(target: ApiTarget, path: string, body: unknown): Observable<T> {
    return this.http.put<T>(this.url(target, path), body);
  }

  delete<T>(target: ApiTarget, path: string): Observable<T> {
    return this.http.delete<T>(this.url(target, path));
  }

  private url(target: ApiTarget, path: string): string {
    const base =
      target === 'platform'
        ? this.runtimeConfig.platformApiUrl()
        : this.runtimeConfig.agentRuntimeUrl();
    const normalizedBase = base.replace(/\/$/, '');
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${normalizedBase}${normalizedPath}`;
  }

  private toParams(params?: Record<string, string>): HttpParams | undefined {
    if (!params) {
      return undefined;
    }
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      httpParams = httpParams.set(key, value);
    }
    return httpParams;
  }
}
