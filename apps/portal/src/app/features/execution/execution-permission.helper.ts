import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const EXECUTION_PERMISSIONS = {
  execute: 'AGENT_EXECUTE',
  read: 'EXECUTION_READ',
  cancel: 'EXECUTION_CANCEL',
} as const;

@Injectable({ providedIn: 'root' })
export class ExecutionPermissionHelper {
  private readonly session = inject(UserSessionService);

  canExecute(): boolean {
    return this.has(EXECUTION_PERMISSIONS.execute);
  }

  canRead(): boolean {
    return this.has(EXECUTION_PERMISSIONS.read);
  }

  canCancel(): boolean {
    return this.has(EXECUTION_PERMISSIONS.cancel);
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
