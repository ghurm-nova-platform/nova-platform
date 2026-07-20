import { TestBed } from '@angular/core/testing';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PolicyPermissionHelper } from './policy-permission.helper';

describe('PolicyPermissionHelper', () => {
  it('allows ORG_ADMIN', () => {
    TestBed.configureTestingModule({
      providers: [
        PolicyPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(PolicyPermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        PolicyPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['POLICY_READ'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(PolicyPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
