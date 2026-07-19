import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { DeploymentPermissionHelper } from './deployment-permission.helper';

describe('DeploymentPermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        DeploymentPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(DeploymentPermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
  });

  it('checks DEPLOYMENT permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        DeploymentPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['USER'],
              permissions: ['DEPLOYMENT_READ'],
            }),
          },
        },
      ],
    });
    const helper = TestBed.inject(DeploymentPermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
