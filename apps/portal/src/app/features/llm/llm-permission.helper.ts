import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class LlmPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has('LLM_READ');
  }

  canInfer(): boolean {
    return this.has('LLM_INFER');
  }

  canAdmin(): boolean {
    return this.has('LLM_ADMIN');
  }

  canManageModels(): boolean {
    return this.has('LLM_MODEL_ADMIN');
  }

  canManagePrompts(): boolean {
    return this.has('LLM_PROMPT_ADMIN');
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
