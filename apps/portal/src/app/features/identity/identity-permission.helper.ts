import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

@Injectable({ providedIn: 'root' })
export class IdentityPermissionHelper {
  private readonly session = inject(UserSessionService);

  canRead(): boolean {
    return this.has('IDENTITY_READ');
  }

  canAdmin(): boolean {
    return this.has('IDENTITY_ADMIN');
  }

  canManageProviders(): boolean {
    return this.has('IDENTITY_PROVIDER_MANAGE') || this.has('IDENTITY_ADMIN');
  }

  canManageMfa(): boolean {
    return this.has('IDENTITY_MFA_MANAGE') || this.has('IDENTITY_ADMIN');
  }

  canProvisionScim(): boolean {
    return this.has('SCIM_PROVISION') || this.has('IDENTITY_ADMIN');
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
