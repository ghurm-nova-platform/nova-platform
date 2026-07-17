import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const PROMPT_PERMISSIONS = {
  read: 'PROMPT_READ',
  create: 'PROMPT_CREATE',
  update: 'PROMPT_UPDATE',
  publish: 'PROMPT_PUBLISH',
  archive: 'PROMPT_ARCHIVE',
  compare: 'PROMPT_COMPARE',
  preview: 'PROMPT_PREVIEW',
} as const;

@Injectable({ providedIn: 'root' })
export class PromptPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has(PROMPT_PERMISSIONS.read);
  }

  canCreate(): boolean {
    return this.has(PROMPT_PERMISSIONS.create);
  }

  canUpdate(): boolean {
    return this.has(PROMPT_PERMISSIONS.update);
  }

  canPublish(): boolean {
    return this.has(PROMPT_PERMISSIONS.publish);
  }

  canArchive(): boolean {
    return this.has(PROMPT_PERMISSIONS.archive);
  }

  canCompare(): boolean {
    return this.has(PROMPT_PERMISSIONS.compare);
  }

  canPreview(): boolean {
    return this.has(PROMPT_PERMISSIONS.preview);
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
