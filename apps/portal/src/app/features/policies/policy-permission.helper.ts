import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class PolicyPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRun(): boolean {
    return this.has('POLICY_RUN');
  }

  canRead(): boolean {
    return this.has('POLICY_READ');
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
