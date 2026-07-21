import { TestBed } from '@angular/core/testing';

import { AuthUser } from '../../auth/services/auth.models';
import { UserSessionService } from '../../auth/services/user-session.service';
import { IdentityPermissionHelper } from './identity-permission.helper';

describe('PermissionGuardTest', () => {
  let helper: IdentityPermissionHelper;
  let session: UserSessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    helper = TestBed.inject(IdentityPermissionHelper);
    session = TestBed.inject(UserSessionService);
  });

  afterEach(() => {
    session.clear();
  });

  it('denies identity read when no user is signed in', () => {
    expect(helper.canRead()).toBeFalse();
  });

  it('grants identity read to ORG_ADMIN', () => {
    const admin: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444401',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'admin@nova.local',
      displayName: 'Nova Admin',
      roles: ['ORG_ADMIN'],
      permissions: [],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, admin);

    expect(helper.canRead()).toBeTrue();
    expect(helper.canAdmin()).toBeTrue();
  });

  it('grants identity read for IDENTITY_READ permission', () => {
    const member: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444402',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'member@nova.local',
      displayName: 'Nova Member',
      roles: ['PROJECT_MEMBER'],
      permissions: ['IDENTITY_READ'],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, member);

    expect(helper.canRead()).toBeTrue();
    expect(helper.canAdmin()).toBeFalse();
  });
});
