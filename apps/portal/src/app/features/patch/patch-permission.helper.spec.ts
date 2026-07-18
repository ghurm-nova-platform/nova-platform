import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PatchPermissionHelper } from './patch-permission.helper';

describe('PatchPermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        PatchPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(PatchPermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks PATCH permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        PatchPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['PATCH_READ'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(PatchPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
