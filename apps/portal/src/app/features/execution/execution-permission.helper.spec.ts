import { TestBed } from '@angular/core/testing';

import { UserSessionService } from '../../auth/services/user-session.service';
import { AuthUser } from '../../auth/services/auth.models';
import { ExecutionPermissionHelper } from './execution-permission.helper';

describe('ExecutionPermissionHelper', () => {
  let helper: ExecutionPermissionHelper;
  let session: UserSessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    helper = TestBed.inject(ExecutionPermissionHelper);
    session = TestBed.inject(UserSessionService);
  });

  afterEach(() => {
    session.clear();
  });

  it('denies access when no user is signed in', () => {
    expect(helper.canExecute()).toBeFalse();
    expect(helper.canRead()).toBeFalse();
    expect(helper.canCancel()).toBeFalse();
  });

  it('grants all execution permissions to ORG_ADMIN', () => {
    const admin: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444401',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'admin@nova.local',
      displayName: 'Nova Admin',
      roles: ['ORG_ADMIN'],
      permissions: [],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, admin);

    expect(helper.canExecute()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
    expect(helper.canCancel()).toBeTrue();
  });

  it('checks explicit execution permissions for non-admin users', () => {
    const operator: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444402',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'operator@nova.local',
      displayName: 'Nova Operator',
      roles: ['PROJECT_MEMBER'],
      permissions: ['AGENT_EXECUTE', 'EXECUTION_READ'],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, operator);

    expect(helper.canExecute()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
    expect(helper.canCancel()).toBeFalse();
  });
});
