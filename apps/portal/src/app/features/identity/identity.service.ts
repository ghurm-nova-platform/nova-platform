import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import {
  ApiTokenView,
  AssignGroupsRequest,
  AssignPermissionsRequest,
  AssignRolesRequest,
  CloneRoleRequest,
  CreateApiTokenRequest,
  CreateApiTokenResponse,
  CreateGroupRequest,
  CreatePermissionRequest,
  CreateProviderRequest,
  CreateRoleRequest,
  CreateServiceAccountRequest,
  CreateUserRequest,
  GroupSyncResult,
  IdentityConfigResponse,
  IdentityDashboardStats,
  IdentityGroupView,
  IdentityPermissionView,
  IdentityProviderView,
  IdentityRoleView,
  IdentitySessionView,
  IdentitySummaryResponse,
  IdentityUserView,
  LoginHistoryEntry,
  MfaEnrollRequest,
  MfaEnrollResponse,
  MfaStatusResponse,
  MfaVerifyEnrollmentRequest,
  MfaVerifyEnrollmentResponse,
  ProviderSyncResult,
  ProviderTestResult,
  ResetPasswordRequest,
  ScimUserSummary,
  SecurityEventView,
  ServiceAccountView,
  UpdateGroupRequest,
  UpdatePermissionRequest,
  UpdateProviderRequest,
  UpdateRoleRequest,
  UpdateServiceAccountRequest,
  UpdateUserRequest,
} from './identity.models';

@Injectable({ providedIn: 'root' })
export class IdentityService {
  private readonly api = inject(ApiClient);

  getConfig(): Observable<IdentityConfigResponse> {
    return this.api.get<IdentityConfigResponse>('/api/identity/config');
  }

  getDashboard(): Observable<IdentityDashboardStats> {
    return this.api.get<IdentityDashboardStats>('/api/identity/dashboard');
  }

  getSummary(): Observable<IdentitySummaryResponse> {
    return this.api.get<IdentitySummaryResponse>('/api/identity/summary');
  }

  listProviders(): Observable<IdentityProviderView[]> {
    return this.api.get<IdentityProviderView[]>('/api/identity/providers');
  }

  getProvider(id: string): Observable<IdentityProviderView> {
    return this.api.get<IdentityProviderView>(`/api/identity/providers/${encodeURIComponent(id)}`);
  }

  createProvider(request: CreateProviderRequest): Observable<IdentityProviderView> {
    return this.api.post<IdentityProviderView>('/api/identity/providers', request);
  }

  updateProvider(id: string, request: UpdateProviderRequest): Observable<IdentityProviderView> {
    return this.api.put<IdentityProviderView>(
      `/api/identity/providers/${encodeURIComponent(id)}`,
      request,
    );
  }

  deleteProvider(id: string): Observable<void> {
    return this.api.delete<void>(`/api/identity/providers/${encodeURIComponent(id)}`);
  }

  testProvider(id: string): Observable<ProviderTestResult> {
    return this.api.post<ProviderTestResult>(
      `/api/identity/providers/${encodeURIComponent(id)}/test`,
      {},
    );
  }

  syncProvider(id: string): Observable<ProviderSyncResult> {
    return this.api.post<ProviderSyncResult>(
      `/api/identity/providers/${encodeURIComponent(id)}/sync`,
      {},
    );
  }

  listUsers(): Observable<IdentityUserView[]> {
    return this.api.get<IdentityUserView[]>('/api/identity/users');
  }

  getUser(id: string): Observable<IdentityUserView> {
    return this.api.get<IdentityUserView>(`/api/identity/users/${encodeURIComponent(id)}`);
  }

  createUser(request: CreateUserRequest): Observable<IdentityUserView> {
    return this.api.post<IdentityUserView>('/api/identity/users', request);
  }

  updateUser(id: string, request: UpdateUserRequest): Observable<IdentityUserView> {
    return this.api.put<IdentityUserView>(`/api/identity/users/${encodeURIComponent(id)}`, request);
  }

  disableUser(id: string): Observable<IdentityUserView> {
    return this.api.post<IdentityUserView>(`/api/identity/users/${encodeURIComponent(id)}/disable`, {});
  }

  enableUser(id: string): Observable<IdentityUserView> {
    return this.api.post<IdentityUserView>(`/api/identity/users/${encodeURIComponent(id)}/enable`, {});
  }

  unlockUser(id: string): Observable<IdentityUserView> {
    return this.api.post<IdentityUserView>(`/api/identity/users/${encodeURIComponent(id)}/unlock`, {});
  }

  resetPassword(id: string, request: ResetPasswordRequest): Observable<void> {
    return this.api.post<void>(
      `/api/identity/users/${encodeURIComponent(id)}/reset-password`,
      request,
    );
  }

  assignUserRoles(id: string, request: AssignRolesRequest): Observable<IdentityUserView> {
    return this.api.post<IdentityUserView>(
      `/api/identity/users/${encodeURIComponent(id)}/roles`,
      request,
    );
  }

  assignUserGroups(id: string, request: AssignGroupsRequest): Observable<IdentityUserView> {
    return this.api.post<IdentityUserView>(
      `/api/identity/users/${encodeURIComponent(id)}/groups`,
      request,
    );
  }

  getUserLoginHistory(id: string): Observable<LoginHistoryEntry[]> {
    return this.api.get<LoginHistoryEntry[]>(
      `/api/identity/users/${encodeURIComponent(id)}/login-history`,
    );
  }

  listGroups(): Observable<IdentityGroupView[]> {
    return this.api.get<IdentityGroupView[]>('/api/identity/groups');
  }

  getGroup(id: string): Observable<IdentityGroupView> {
    return this.api.get<IdentityGroupView>(`/api/identity/groups/${encodeURIComponent(id)}`);
  }

  createGroup(request: CreateGroupRequest): Observable<IdentityGroupView> {
    return this.api.post<IdentityGroupView>('/api/identity/groups', request);
  }

  updateGroup(id: string, request: UpdateGroupRequest): Observable<IdentityGroupView> {
    return this.api.put<IdentityGroupView>(`/api/identity/groups/${encodeURIComponent(id)}`, request);
  }

  deleteGroup(id: string): Observable<void> {
    return this.api.delete<void>(`/api/identity/groups/${encodeURIComponent(id)}`);
  }

  syncGroup(id: string): Observable<GroupSyncResult> {
    return this.api.post<GroupSyncResult>(`/api/identity/groups/${encodeURIComponent(id)}/sync`, {});
  }

  listRoles(): Observable<IdentityRoleView[]> {
    return this.api.get<IdentityRoleView[]>('/api/identity/roles');
  }

  getRole(id: string): Observable<IdentityRoleView> {
    return this.api.get<IdentityRoleView>(`/api/identity/roles/${encodeURIComponent(id)}`);
  }

  createRole(request: CreateRoleRequest): Observable<IdentityRoleView> {
    return this.api.post<IdentityRoleView>('/api/identity/roles', request);
  }

  updateRole(id: string, request: UpdateRoleRequest): Observable<IdentityRoleView> {
    return this.api.put<IdentityRoleView>(`/api/identity/roles/${encodeURIComponent(id)}`, request);
  }

  deleteRole(id: string): Observable<void> {
    return this.api.delete<void>(`/api/identity/roles/${encodeURIComponent(id)}`);
  }

  cloneRole(id: string, request: CloneRoleRequest): Observable<IdentityRoleView> {
    return this.api.post<IdentityRoleView>(
      `/api/identity/roles/${encodeURIComponent(id)}/clone`,
      request,
    );
  }

  assignRolePermissions(id: string, request: AssignPermissionsRequest): Observable<IdentityRoleView> {
    return this.api.post<IdentityRoleView>(
      `/api/identity/roles/${encodeURIComponent(id)}/permissions`,
      request,
    );
  }

  listPermissions(query?: string): Observable<IdentityPermissionView[]> {
    const params = query ? { q: query } : undefined;
    return this.api.get<IdentityPermissionView[]>('/api/identity/permissions', params);
  }

  getPermission(id: string): Observable<IdentityPermissionView> {
    return this.api.get<IdentityPermissionView>(
      `/api/identity/permissions/${encodeURIComponent(id)}`,
    );
  }

  createPermission(request: CreatePermissionRequest): Observable<IdentityPermissionView> {
    return this.api.post<IdentityPermissionView>('/api/identity/permissions', request);
  }

  updatePermission(id: string, request: UpdatePermissionRequest): Observable<IdentityPermissionView> {
    return this.api.put<IdentityPermissionView>(
      `/api/identity/permissions/${encodeURIComponent(id)}`,
      request,
    );
  }

  deletePermission(id: string): Observable<void> {
    return this.api.delete<void>(`/api/identity/permissions/${encodeURIComponent(id)}`);
  }

  listSessions(): Observable<IdentitySessionView[]> {
    return this.api.get<IdentitySessionView[]>('/api/identity/sessions');
  }

  revokeSession(id: string): Observable<void> {
    return this.api.post<void>(`/api/identity/sessions/${encodeURIComponent(id)}/revoke`, {});
  }

  revokeAllSessions(userId?: string): Observable<void> {
    const path = userId
      ? `/api/identity/sessions/revoke-all?userId=${encodeURIComponent(userId)}`
      : '/api/identity/sessions/revoke-all';
    return this.api.post<void>(path, {});
  }

  loginHistory(): Observable<LoginHistoryEntry[]> {
    return this.api.get<LoginHistoryEntry[]>('/api/identity/login-history');
  }

  listApiTokens(): Observable<ApiTokenView[]> {
    return this.api.get<ApiTokenView[]>('/api/identity/api-tokens');
  }

  createApiToken(request: CreateApiTokenRequest): Observable<CreateApiTokenResponse> {
    return this.api.post<CreateApiTokenResponse>('/api/identity/api-tokens', request);
  }

  revokeApiToken(id: string): Observable<void> {
    return this.api.post<void>(`/api/identity/api-tokens/${encodeURIComponent(id)}/revoke`, {});
  }

  listServiceAccounts(): Observable<ServiceAccountView[]> {
    return this.api.get<ServiceAccountView[]>('/api/identity/service-accounts');
  }

  getServiceAccount(id: string): Observable<ServiceAccountView> {
    return this.api.get<ServiceAccountView>(
      `/api/identity/service-accounts/${encodeURIComponent(id)}`,
    );
  }

  createServiceAccount(request: CreateServiceAccountRequest): Observable<ServiceAccountView> {
    return this.api.post<ServiceAccountView>('/api/identity/service-accounts', request);
  }

  updateServiceAccount(
    id: string,
    request: UpdateServiceAccountRequest,
  ): Observable<ServiceAccountView> {
    return this.api.put<ServiceAccountView>(
      `/api/identity/service-accounts/${encodeURIComponent(id)}`,
      request,
    );
  }

  deleteServiceAccount(id: string): Observable<void> {
    return this.api.delete<void>(`/api/identity/service-accounts/${encodeURIComponent(id)}`);
  }

  listSecurityEvents(): Observable<SecurityEventView[]> {
    return this.api.get<SecurityEventView[]>('/api/identity/security-events');
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
