import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { TestingPermissionHelper } from './testing-permission.helper';

describe('TestingPermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        TestingPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(TestingPermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks TESTING permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        TestingPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['TESTING_READ'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(TestingPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
