import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class PrReviewPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has('PR_REVIEW_READ');
  }

  canRun(): boolean {
    return this.has('PR_REVIEW_RUN');
  }

  canAdmin(): boolean {
    return this.has('PR_REVIEW_ADMIN');
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
