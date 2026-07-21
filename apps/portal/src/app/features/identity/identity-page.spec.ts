import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import {
  IdentityPage,
  activeSessionCount,
  passwordPolicySummary,
} from './identity-page';
import { IdentityService } from './identity.service';
import {
  IdentityConfigResponse,
  IdentityProviderView,
  IdentitySessionView,
  LoginHistoryEntry,
  MfaStatusResponse,
} from './identity.models';

describe('IdentityComponentTest', () => {
  let fixture: ComponentFixture<IdentityPage>;

  const config: IdentityConfigResponse = {
    enabled: true,
    jwtAccessTtlSeconds: 900,
    jwtRefreshTtlSeconds: 604800,
    passwordMinLength: 12,
    passwordRequireUppercase: true,
    passwordRequireLowercase: true,
    passwordRequireDigit: true,
    passwordRequireSpecial: true,
    passwordMaxAgeDays: 90,
    sessionMaxConcurrent: 5,
    mfaRequired: true,
    scimEnabled: true,
    samlEnabled: true,
    oidcEnabled: false,
    ldapEnabled: false,
  };

  const provider: IdentityProviderView = {
    id: 'prov-1',
    organizationId: 'org-1',
    type: 'SAML',
    name: 'corp-saml',
    status: 'ACTIVE',
    displayName: 'Corporate SAML',
    issuerUrl: 'https://idp.example.com',
    defaultProvider: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const session: IdentitySessionView = {
    id: 'sess-1',
    userId: 'user-1',
    userEmail: 'admin@nova.local',
    userDisplayName: 'Admin User',
    status: 'ACTIVE',
    ipAddress: '127.0.0.1',
    userAgent: 'Chrome',
    authMethod: 'LOCAL',
    createdAt: new Date().toISOString(),
    lastSeenAt: new Date().toISOString(),
    expiresAt: new Date(Date.now() + 3600000).toISOString(),
    current: true,
  };

  const loginEntry: LoginHistoryEntry = {
    id: 'log-1',
    userId: 'user-1',
    userEmail: 'admin@nova.local',
    outcome: 'SUCCESS',
    authMethod: 'LOCAL',
    ipAddress: '127.0.0.1',
    occurredAt: new Date().toISOString(),
  };

  const mfaStatus: MfaStatusResponse = {
    status: 'ENROLLED',
    enrolledMethods: ['TOTP'],
    required: true,
    lastVerifiedAt: new Date().toISOString(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({
              roles: ['USER'],
              permissions: [
                'IDENTITY_READ',
                'IDENTITY_ADMIN',
                'IDENTITY_MFA_MANAGE',
                'SCIM_PROVISION',
              ],
            }),
          },
        },
        {
          provide: IdentityService,
          useValue: {
            getConfig: () => of(config),
            listProviders: () => of([provider]),
            listSessions: () => of([session]),
            loginHistory: () => of([loginEntry]),
            mfaStatus: () => of(mfaStatus),
            listScimUsers: () => of([]),
            revokeSession: () => of(undefined),
            mfaEnroll: () =>
              of({
                method: 'TOTP',
                secret: 'SECRET123',
                qrCodeUri: 'otpauth://totp/Nova',
                enrollmentToken: 'token-1',
              }),
            mfaVerifyEnrollment: () => of({ status: 'ENROLLED', backupCodes: ['abc'] }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityPage);
    fixture.detectChanges();
  });

  it('renders Enterprise Identity heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Enterprise Identity');
  });

  it('renders provider data and loads sessions', () => {
    expect(fixture.nativeElement.textContent).toContain('Corporate SAML');
    expect(fixture.componentInstance.sessions().length).toBe(1);
    expect(fixture.componentInstance.sessions()[0].userDisplayName).toBe('Admin User');
  });

  it('counts active sessions', () => {
    const active: IdentitySessionView = { ...session, id: 's-2', current: false };
    const revoked: IdentitySessionView = { ...session, id: 's-3', status: 'REVOKED' };
    expect(activeSessionCount([session, active, revoked])).toBe(2);
  });

  it('summarizes password policy', () => {
    const summary = passwordPolicySummary(config);
    expect(summary).toContain('min 12 chars');
    expect(summary).toContain('uppercase');
    expect(summary).toContain('max age 90 days');
  });
});
