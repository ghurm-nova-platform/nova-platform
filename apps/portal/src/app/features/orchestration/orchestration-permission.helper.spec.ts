import { TestBed } from '@angular/core/testing';

import { UserSessionService } from '../../auth/services/user-session.service';
import { AuthUser } from '../../auth/services/auth.models';
import { OrchestrationPermissionHelper } from './orchestration-permission.helper';

describe('OrchestrationPermissionHelper', () => {
  let helper: OrchestrationPermissionHelper;
  let session: UserSessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    helper = TestBed.inject(OrchestrationPermissionHelper);
    session = TestBed.inject(UserSessionService);
  });

  afterEach(() => {
    session.clear();
  });

  it('denies access when no user is signed in', () => {
    expect(helper.canReadRuns()).toBeFalse();
    expect(helper.canCreateRun()).toBeFalse();
    expect(helper.canManageTasks()).toBeFalse();
    expect(helper.canReadEvents()).toBeFalse();
  });

  it('grants all orchestration permissions to ORG_ADMIN', () => {
    const admin: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444401',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'admin@nova.local',
      displayName: 'Nova Admin',
      roles: ['ORG_ADMIN'],
      permissions: [],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, admin);

    expect(helper.canReadRuns()).toBeTrue();
    expect(helper.canCreateRun()).toBeTrue();
    expect(helper.canUpdateRun()).toBeTrue();
    expect(helper.canStartRun()).toBeTrue();
    expect(helper.canCancelRun()).toBeTrue();
    expect(helper.canArchiveRun()).toBeTrue();
    expect(helper.canManageTasks()).toBeTrue();
    expect(helper.canExecuteTasks()).toBeTrue();
    expect(helper.canReadEvents()).toBeTrue();
  });

  it('checks explicit orchestration permissions for non-admin users', () => {
    const member: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444402',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'member@nova.local',
      displayName: 'Nova Member',
      roles: ['PROJECT_MEMBER'],
      permissions: ['ORCHESTRATION_RUN_READ', 'ORCHESTRATION_EVENT_READ'],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, member);

    expect(helper.canReadRuns()).toBeTrue();
    expect(helper.canReadEvents()).toBeTrue();
    expect(helper.canCreateRun()).toBeFalse();
    expect(helper.canUpdateRun()).toBeFalse();
    expect(helper.canStartRun()).toBeFalse();
    expect(helper.canCancelRun()).toBeFalse();
    expect(helper.canArchiveRun()).toBeFalse();
    expect(helper.canManageTasks()).toBeFalse();
    expect(helper.canExecuteTasks()).toBeFalse();
  });
});
