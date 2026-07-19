import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class ApprovalGatePermissionHelper {
  private readonly session = inject(UserSessionService);

  canRun(): boolean {
    return this.has('APPROVAL_GATE_RUN');
  }

  canRead(): boolean {
    return this.has('APPROVAL_GATE_READ');
  }

  canApprove(): boolean {
    return this.has('APPROVAL_GATE_APPROVE');
  }

  canReject(): boolean {
    return this.has('APPROVAL_GATE_REJECT');
  }

  private has(permission: string): boolean {
    const user = this.session.user();
    if (!user) {
      return false;
    }
    if (user.roles.includes('ORG_ADMIN')) {
      return true;
    }
    return (user.permissions ?? []).includes(permission);
  }
}
