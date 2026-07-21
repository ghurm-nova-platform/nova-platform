import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { IdentityPermissionHelper } from './identity-permission.helper';

export const identityPermissionGuard: CanActivateFn = () => {
  const permissions = inject(IdentityPermissionHelper);
  const router = inject(Router);

  if (permissions.canRead()) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
