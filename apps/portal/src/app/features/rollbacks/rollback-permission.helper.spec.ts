import { TestBed } from '@angular/core/testing';

import { UserSessionService } from '../../auth/services/user-session.service';
import { RollbackPermissionHelper } from './rollback-permission.helper';

describe('RollbackPermissionHelper', () => {
  it('allows ORG_ADMIN', () => {
    TestBed.configureTestingModule({
      providers: [
        RollbackPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(RollbackPermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        RollbackPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['ROLLBACK_READ'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(RollbackPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
