import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const AGENT_PERMISSIONS = {
  read: 'AGENT_READ',
  create: 'AGENT_CREATE',
  update: 'AGENT_UPDATE',
  activate: 'AGENT_ACTIVATE',
  archive: 'AGENT_ARCHIVE',
} as const;

@Injectable({ providedIn: 'root' })
export class AgentPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has(AGENT_PERMISSIONS.read);
  }

  canCreate(): boolean {
    return this.has(AGENT_PERMISSIONS.create);
  }

  canUpdate(): boolean {
    return this.has(AGENT_PERMISSIONS.update);
  }

  canActivate(): boolean {
    return this.has(AGENT_PERMISSIONS.activate);
  }

  canArchive(): boolean {
    return this.has(AGENT_PERMISSIONS.archive);
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
