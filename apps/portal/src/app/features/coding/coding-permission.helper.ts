import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class CodingPermissionHelper {
  private readonly session = inject(UserSessionService);

  canGenerate(): boolean {
    return this.has('CODING_GENERATE');
  }

  canRead(): boolean {
    return this.has('CODING_READ');
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
