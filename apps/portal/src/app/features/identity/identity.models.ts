export type IdentityProviderType = 'LOCAL' | 'SAML' | 'OIDC' | 'LDAP';

export type IdentityProviderStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

export type SessionStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export type LoginOutcome = 'SUCCESS' | 'FAILURE' | 'MFA_REQUIRED' | 'LOCKED';

export type MfaMethod = 'TOTP' | 'SMS';

export type MfaEnrollmentStatus = 'NOT_ENROLLED' | 'PENDING' | 'ENROLLED' | 'DISABLED';

export type IdentityUserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED' | 'PENDING';

export type ApiTokenStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export type ServiceAccountStatus = 'ACTIVE' | 'DISABLED';

export type SecurityEventType =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILURE'
  | 'LOGOUT'
  | 'MFA_CHALLENGE'
  | 'ACCOUNT_LOCKED'
  | 'PASSWORD_RESET'
  | 'SESSION_REVOKED';

export interface IdentityConfigResponse {
  enabled: boolean;
  jwtAccessTtlSeconds: number;
  jwtRefreshTtlSeconds: number;
  passwordMinLength: number;
  passwordRequireUppercase: boolean;
  passwordRequireLowercase: boolean;
  passwordRequireDigit: boolean;
  passwordRequireSpecial: boolean;
  passwordMaxAgeDays: number;
  sessionMaxConcurrent: number;
  mfaRequired: boolean;
  scimEnabled: boolean;
  samlEnabled: boolean;
  oidcEnabled: boolean;
  ldapEnabled: boolean;
}

export interface IdentityDashboardStats {
  activeUsers: number;
  onlineSessions: number;
  failedLogins24h: number;
  lockedAccounts: number;
  mfaAdoptionPercent: number;
  providerCount: number;
  recentLogins: LoginHistoryEntry[];
  securityAlerts: SecurityEventView[];
}

export interface IdentityProviderView {
  id: string;
  organizationId: string;
  type: IdentityProviderType;
  name: string;
  status: IdentityProviderStatus;
  displayName: string;
  issuerUrl?: string | null;
  clientId?: string | null;
  metadataUrl?: string | null;
  defaultProvider: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProviderRequest {
  type: IdentityProviderType;
  name: string;
  displayName?: string;
  issuerUrl?: string;
  clientId?: string;
  metadataUrl?: string;
  defaultProvider?: boolean;
  configJson?: string;
}

export interface UpdateProviderRequest extends Partial<CreateProviderRequest> {
  status?: IdentityProviderStatus;
}

export interface ProviderTestResult {
  success: boolean;
  message: string;
  testedAt: string;
}

export interface ProviderSyncResult {
  usersCreated: number;
  usersUpdated: number;
  groupsCreated: number;
  groupsUpdated: number;
  syncedAt: string;
}

export interface IdentitySessionView {
  id: string;
  userId: string;
  userEmail: string;
  userDisplayName?: string | null;
  status: SessionStatus;
  ipAddress?: string | null;
  userAgent?: string | null;
  authMethod: string;
  providerId?: string | null;
  providerName?: string | null;
  createdAt: string;
  lastSeenAt: string;
  expiresAt: string;
  current: boolean;
}

export interface LoginHistoryEntry {
  id: string;
  userId: string;
  userEmail: string;
  outcome: LoginOutcome;
  authMethod: string;
  providerId?: string | null;
  providerName?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  failureReason?: string | null;
  occurredAt: string;
}

export interface IdentityUserView {
  id: string;
  organizationId: string;
  email: string;
  displayName: string;
  status: IdentityUserStatus;
  mfaEnabled: boolean;
  locked: boolean;
  roles: string[];
  groups: string[];
  lastLoginAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  email: string;
  displayName: string;
  password?: string;
  roles?: string[];
  groups?: string[];
}

export interface UpdateUserRequest {
  displayName?: string;
  email?: string;
  roles?: string[];
  groups?: string[];
}

export interface ResetPasswordRequest {
  newPassword: string;
  forceChangeOnLogin?: boolean;
}

export interface AssignRolesRequest {
  roleIds: string[];
}

export interface AssignGroupsRequest {
  groupIds: string[];
}

export interface IdentityGroupView {
  id: string;
  organizationId: string;
  name: string;
  description?: string | null;
  memberCount: number;
  externalId?: string | null;
  syncedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
}

export interface UpdateGroupRequest {
  name?: string;
  description?: string;
}

export interface GroupSyncResult {
  membersAdded: number;
  membersRemoved: number;
  syncedAt: string;
}

export interface IdentityRoleView {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  description?: string | null;
  systemRole: boolean;
  permissions: string[];
  userCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRoleRequest {
  code: string;
  name: string;
  description?: string;
  permissions?: string[];
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
  permissions?: string[];
}

export interface CloneRoleRequest {
  code: string;
  name: string;
}

export interface AssignPermissionsRequest {
  permissionCodes: string[];
}

export interface IdentityPermissionView {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  category?: string | null;
  systemPermission: boolean;
  createdAt: string;
}

export interface CreatePermissionRequest {
  code: string;
  name: string;
  description?: string;
  category?: string;
}

export interface UpdatePermissionRequest {
  name?: string;
  description?: string;
  category?: string;
}

export interface ApiTokenView {
  id: string;
  name: string;
  prefix: string;
  status: ApiTokenStatus;
  scopes: string[];
  lastUsedAt?: string | null;
  expiresAt?: string | null;
  createdAt: string;
  createdBy?: string | null;
}

export interface CreateApiTokenRequest {
  name: string;
  scopes?: string[];
  expiresAt?: string;
}

export interface CreateApiTokenResponse {
  token: ApiTokenView;
  secret: string;
}

export interface ServiceAccountView {
  id: string;
  organizationId: string;
  name: string;
  description?: string | null;
  status: ServiceAccountStatus;
  roles: string[];
  lastUsedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateServiceAccountRequest {
  name: string;
  description?: string;
  roles?: string[];
}

export interface UpdateServiceAccountRequest {
  name?: string;
  description?: string;
  roles?: string[];
}

export interface SecurityEventView {
  id: string;
  type: SecurityEventType;
  userId?: string | null;
  userEmail?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  message: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  occurredAt: string;
}

export interface MfaStatusResponse {
  status: MfaEnrollmentStatus;
  enrolledMethods: MfaMethod[];
  required: boolean;
  backupCodesRemaining?: number | null;
  lastVerifiedAt?: string | null;
}

export interface MfaEnrollRequest {
  method: MfaMethod;
}

export interface MfaEnrollResponse {
  method: MfaMethod;
  secret?: string | null;
  qrCodeUri?: string | null;
  enrollmentToken: string;
}

export interface MfaVerifyEnrollmentRequest {
  enrollmentToken: string;
  code: string;
}

export interface MfaVerifyEnrollmentResponse {
  status: MfaEnrollmentStatus;
  backupCodes?: string[] | null;
}

export interface ScimUserSummary {
  id: string;
  userName: string;
  displayName?: string | null;
  active: boolean;
  externalId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface IdentitySummaryResponse {
  users: IdentityUserView[];
  groups: IdentityGroupView[];
  providers: IdentityProviderView[];
}
