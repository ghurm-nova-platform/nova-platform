import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const CONVERSATION_PERMISSIONS = {
  read: 'CONVERSATION_READ',
  create: 'CONVERSATION_CREATE',
  update: 'CONVERSATION_UPDATE',
  archive: 'CONVERSATION_ARCHIVE',
  messageRead: 'CONVERSATION_MESSAGE_READ',
  messageCreate: 'CONVERSATION_MESSAGE_CREATE',
} as const;

@Injectable({ providedIn: 'root' })
export class ConversationPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has(CONVERSATION_PERMISSIONS.read);
  }

  canCreate(): boolean {
    return this.has(CONVERSATION_PERMISSIONS.create);
  }

  canUpdate(): boolean {
    return this.has(CONVERSATION_PERMISSIONS.update);
  }

  canArchive(): boolean {
    return this.has(CONVERSATION_PERMISSIONS.archive);
  }

  canReadMessages(): boolean {
    return this.has(CONVERSATION_PERMISSIONS.messageRead);
  }

  canCreateMessage(): boolean {
    return this.has(CONVERSATION_PERMISSIONS.messageCreate);
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
