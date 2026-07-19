import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { CiPermissionHelper } from './ci-permission.helper';

describe('CiPermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        CiPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(CiPermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks CI permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        CiPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['CI_READ'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(CiPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
