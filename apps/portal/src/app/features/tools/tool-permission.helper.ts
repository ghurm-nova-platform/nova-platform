import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const TOOL_PERMISSIONS = {
  read: 'TOOL_READ',
  create: 'TOOL_CREATE',
  update: 'TOOL_UPDATE',
  activate: 'TOOL_ACTIVATE',
  archive: 'TOOL_ARCHIVE',
  assign: 'TOOL_ASSIGN',
  execute: 'TOOL_EXECUTE',
  callRead: 'TOOL_CALL_READ',
  callApprove: 'TOOL_CALL_APPROVE',
} as const;

@Injectable({ providedIn: 'root' })
export class ToolPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has(TOOL_PERMISSIONS.read);
  }

  canCreate(): boolean {
    return this.has(TOOL_PERMISSIONS.create);
  }

  canUpdate(): boolean {
    return this.has(TOOL_PERMISSIONS.update);
  }

  canActivate(): boolean {
    return this.has(TOOL_PERMISSIONS.activate);
  }

  canArchive(): boolean {
    return this.has(TOOL_PERMISSIONS.archive);
  }

  canAssign(): boolean {
    return this.has(TOOL_PERMISSIONS.assign);
  }

  canExecute(): boolean {
    return this.has(TOOL_PERMISSIONS.execute);
  }

  canReadToolCalls(): boolean {
    return this.has(TOOL_PERMISSIONS.callRead);
  }

  canApproveToolCalls(): boolean {
    return this.has(TOOL_PERMISSIONS.callApprove);
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
