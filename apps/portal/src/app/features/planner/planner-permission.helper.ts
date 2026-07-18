import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class PlannerPermissionHelper {
  private readonly session = inject(UserSessionService);

  canPlan(): boolean {
    return this.has('PLANNER_PLAN');
  }

  canImport(): boolean {
    return this.has('PLANNER_IMPORT');
  }

  canReadTemplates(): boolean {
    return this.has('PLANNER_TEMPLATE_READ');
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
