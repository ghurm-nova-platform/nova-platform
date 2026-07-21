import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentityDashboardPage } from './identity-dashboard-page';
import { IdentityService } from '../identity.service';
import {
  IdentityConfigResponse,
  IdentityDashboardStats,
  IdentitySessionView,
  LoginHistoryEntry,
} from '../identity.models';
import { activeSessionCount, passwordPolicySummary } from '../identity.utils';

describe('IdentityDashboardTest', () => {
  let fixture: ComponentFixture<IdentityDashboardPage>;

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

  const loginEntry: LoginHistoryEntry = {
    id: 'log-1',
    userId: 'user-1',
    userEmail: 'admin@nova.local',
    outcome: 'SUCCESS',
    authMethod: 'LOCAL',
    ipAddress: '127.0.0.1',
    occurredAt: new Date().toISOString(),
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

  const dashboard: IdentityDashboardStats = {
    activeUsers: 12,
    onlineSessions: 3,
    failedLogins24h: 2,
    lockedAccounts: 1,
    mfaAdoptionPercent: 75,
    providerCount: 2,
    recentLogins: [loginEntry],
    securityAlerts: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityDashboardPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            getDashboard: () => of(dashboard),
            getSummary: () => of(null),
            listSessions: () => of([session]),
            loginHistory: () => of([loginEntry]),
            listProviders: () => of([]),
            listSecurityEvents: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityDashboardPage);
    fixture.detectChanges();
  });

  it('renders dashboard stats', () => {
    expect(fixture.nativeElement.textContent).toContain('Active Users');
    expect(fixture.nativeElement.textContent).toContain('12');
    expect(fixture.nativeElement.textContent).toContain('admin@nova.local');
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
