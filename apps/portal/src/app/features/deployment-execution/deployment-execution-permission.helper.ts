import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class DeploymentExecutionPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRun(): boolean {
    return this.has('EXECUTION_RUN');
  }

  canRead(): boolean {
    return this.has('EXECUTION_READ');
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
