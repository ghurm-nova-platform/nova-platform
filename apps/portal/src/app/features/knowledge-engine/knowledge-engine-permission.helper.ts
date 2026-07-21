import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class KnowledgeEnginePermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has('KNOWLEDGE_READ');
  }

  canWrite(): boolean {
    return this.has('KNOWLEDGE_WRITE');
  }

  canAdmin(): boolean {
    return this.has('KNOWLEDGE_ADMIN');
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
