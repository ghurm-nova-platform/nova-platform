import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PullRequestPermissionHelper } from './pull-request-permission.helper';

describe('PullRequestPermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        PullRequestPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(PullRequestPermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks PR permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        PullRequestPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['PR_READ'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(PullRequestPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
