import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  IdentityConfigResponse,
  IdentityProviderView,
  IdentitySessionView,
  LoginHistoryEntry,
  MfaEnrollRequest,
  MfaEnrollResponse,
  MfaStatusResponse,
  MfaVerifyEnrollmentRequest,
  MfaVerifyEnrollmentResponse,
  ScimUserSummary,
} from './identity.models';

@Injectable({ providedIn: 'root' })
export class IdentityService {
  private readonly api = inject(ApiClient);

  getConfig(): Observable<IdentityConfigResponse> {
    return this.api.get<IdentityConfigResponse>('/api/identity/config');
  }

  listProviders(): Observable<IdentityProviderView[]> {
    return this.api.get<IdentityProviderView[]>('/api/identity/providers');
  }

  listSessions(): Observable<IdentitySessionView[]> {
    return this.api.get<IdentitySessionView[]>('/api/identity/sessions');
  }

  revokeSession(id: string): Observable<void> {
    return this.api.post<void>(`/api/identity/sessions/${encodeURIComponent(id)}/revoke`, {});
  }

  loginHistory(): Observable<LoginHistoryEntry[]> {
    return this.api.get<LoginHistoryEntry[]>('/api/identity/login-history');
  }

  mfaStatus(): Observable<MfaStatusResponse> {
    return this.api.get<MfaStatusResponse>('/api/identity/mfa/status');
  }

  mfaEnroll(request: MfaEnrollRequest): Observable<MfaEnrollResponse> {
    return this.api.post<MfaEnrollResponse>('/api/identity/mfa/enroll', request);
  }

  mfaVerifyEnrollment(
    request: MfaVerifyEnrollmentRequest,
  ): Observable<MfaVerifyEnrollmentResponse> {
    return this.api.post<MfaVerifyEnrollmentResponse>(
      '/api/identity/mfa/verify-enrollment',
      request,
    );
  }

  listScimUsers(): Observable<ScimUserSummary[]> {
    return this.api.get<ScimUserSummary[]>('/api/scim/v2/Users');
  }
}
