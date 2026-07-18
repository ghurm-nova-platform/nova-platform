import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const ORCHESTRATION_PERMISSIONS = {
  runRead: 'ORCHESTRATION_RUN_READ',
  runCreate: 'ORCHESTRATION_RUN_CREATE',
  runUpdate: 'ORCHESTRATION_RUN_UPDATE',
  runStart: 'ORCHESTRATION_RUN_START',
  runCancel: 'ORCHESTRATION_RUN_CANCEL',
  runArchive: 'ORCHESTRATION_RUN_ARCHIVE',
  taskManage: 'ORCHESTRATION_TASK_MANAGE',
  taskExecute: 'ORCHESTRATION_TASK_EXECUTE',
  eventRead: 'ORCHESTRATION_EVENT_READ',
} as const;

@Injectable({ providedIn: 'root' })
export class OrchestrationPermissionHelper {
  private readonly session = inject(UserSessionService);

  canReadRuns(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.runRead);
  }

  canCreateRun(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.runCreate);
  }

  canUpdateRun(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.runUpdate);
  }

  canStartRun(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.runStart);
  }

  canCancelRun(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.runCancel);
  }

  canArchiveRun(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.runArchive);
  }

  canManageTasks(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.taskManage);
  }

  canExecuteTasks(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.taskExecute);
  }

  canReadEvents(): boolean {
    return this.has(ORCHESTRATION_PERMISSIONS.eventRead);
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
