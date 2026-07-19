import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { ApprovalGatePermissionHelper } from './approval-gate-permission.helper';

describe('ApprovalGatePermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        ApprovalGatePermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(ApprovalGatePermissionHelper);
    expect(helper.canRun()).toBeTrue();
    expect(helper.canRead()).toBeTrue();
    expect(helper.canApprove()).toBeTrue();
    expect(helper.canReject()).toBeTrue();
  });

  it('checks APPROVAL_GATE permissions for non-admin', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        ApprovalGatePermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['USER'],
              permissions: ['APPROVAL_GATE_READ', 'APPROVAL_GATE_APPROVE'],
            }),
          },
        },
      ],
    });
    const helper = TestBed.inject(ApprovalGatePermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canApprove()).toBeTrue();
    expect(helper.canRun()).toBeFalse();
    expect(helper.canReject()).toBeFalse();
  });
});
