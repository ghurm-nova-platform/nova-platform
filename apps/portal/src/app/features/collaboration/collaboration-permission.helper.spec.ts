import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { CollaborationPermissionHelper } from './collaboration-permission.helper';

describe('CollaborationPermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        CollaborationPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(CollaborationPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canWrite()).toBeTrue();
    expect(helper.canAdmin()).toBeTrue();
  });

  it('requires explicit permissions for non-admin users', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        CollaborationPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['MEMBER'],
              permissions: ['COLLABORATION_READ'],
            }),
          },
        },
      ],
    });
    const helper = TestBed.inject(CollaborationPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canWrite()).toBeFalse();
    expect(helper.canAdmin()).toBeFalse();
  });
});
