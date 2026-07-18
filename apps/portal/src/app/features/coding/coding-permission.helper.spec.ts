import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { CodingPermissionHelper } from './coding-permission.helper';

describe('CodingPermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        CodingPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(CodingPermissionHelper);
    expect(helper.canGenerate()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks CODING permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        CodingPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['CODING_READ'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(CodingPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canGenerate()).toBeFalse();
  });
});
