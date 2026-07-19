import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { ReleasePermissionHelper } from './release-permission.helper';

describe('ReleasePermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        ReleasePermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(ReleasePermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks RELEASE permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        ReleasePermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['USER'],
              permissions: ['RELEASE_READ'],
            }),
          },
        },
      ],
    });
    const helper = TestBed.inject(ReleasePermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
