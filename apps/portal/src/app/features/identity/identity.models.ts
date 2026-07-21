export type IdentityProviderType = 'LOCAL' | 'SAML' | 'OIDC' | 'LDAP';

export type IdentityProviderStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

export type SessionStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export type LoginOutcome = 'SUCCESS' | 'FAILURE' | 'MFA_REQUIRED' | 'LOCKED';

export type MfaMethod = 'TOTP' | 'WEBAUTHN' | 'SMS';

export type MfaEnrollmentStatus = 'NOT_ENROLLED' | 'PENDING' | 'ENROLLED' | 'DISABLED';

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
