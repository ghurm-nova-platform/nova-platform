import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const KNOWLEDGE_PERMISSIONS = {
  read: 'KNOWLEDGE_READ',
  create: 'KNOWLEDGE_CREATE',
  update: 'KNOWLEDGE_UPDATE',
  activate: 'KNOWLEDGE_ACTIVATE',
  archive: 'KNOWLEDGE_ARCHIVE',
  documentUpload: 'KNOWLEDGE_DOCUMENT_UPLOAD',
  documentRead: 'KNOWLEDGE_DOCUMENT_READ',
  documentArchive: 'KNOWLEDGE_DOCUMENT_ARCHIVE',
  documentReprocess: 'KNOWLEDGE_DOCUMENT_REPROCESS',
  assign: 'KNOWLEDGE_ASSIGN',
  retrieve: 'KNOWLEDGE_RETRIEVE',
  auditRead: 'KNOWLEDGE_AUDIT_READ',
} as const;

@Injectable({ providedIn: 'root' })
export class KnowledgePermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.read);
  }

  canCreate(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.create);
  }

  canUpdate(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.update);
  }

  canActivate(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.activate);
  }

  canArchive(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.archive);
  }

  canUploadDocuments(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.documentUpload);
  }

  canReadDocuments(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.documentRead);
  }

  canArchiveDocuments(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.documentArchive);
  }

  canReprocessDocuments(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.documentReprocess);
  }

  canAssign(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.assign);
  }

  canRetrieve(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.retrieve);
  }

  canReadAudit(): boolean {
    return this.has(KNOWLEDGE_PERMISSIONS.auditRead);
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
