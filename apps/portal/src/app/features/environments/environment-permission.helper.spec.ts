import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { EnvironmentPermissionHelper } from './environment-permission.helper';

describe('EnvironmentPermissionHelper', () => {
  let helper: EnvironmentPermissionHelper;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        EnvironmentPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['ENVIRONMENT_READ'] }),
          },
        },
      ],
    });
    helper = TestBed.inject(EnvironmentPermissionHelper);
  });

  it('allows read with ENVIRONMENT_READ', () => {
    expect(helper.canRead()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
  });
});
