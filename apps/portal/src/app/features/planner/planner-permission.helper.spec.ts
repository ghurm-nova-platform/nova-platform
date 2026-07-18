import { TestBed } from '@angular/core/testing';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PlannerPermissionHelper } from './planner-permission.helper';

describe('PlannerPermissionHelper', () => {
  it('allows ORG_ADMIN', () => {
    TestBed.configureTestingModule({
      providers: [
        PlannerPermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(PlannerPermissionHelper);
    expect(helper.canPlan()).toBeTrue();
    expect(helper.canImport()).toBeTrue();
  });

  it('checks explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        PlannerPermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: ['PLANNER_PLAN'] }),
          },
        },
      ],
    });
    const helper = TestBed.inject(PlannerPermissionHelper);
    expect(helper.canPlan()).toBeTrue();
    expect(helper.canImport()).toBeFalse();
  });
});
