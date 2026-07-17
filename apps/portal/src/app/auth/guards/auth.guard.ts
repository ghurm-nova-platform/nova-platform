import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { UserSessionService } from '../services/user-session.service';

export const authGuard: CanActivateFn = () => {
  const session = inject(UserSessionService);
  const auth = inject(AuthService);
  const router = inject(Router);

  if (session.isAuthenticated()) {
    return true;
  }

  if (!session.hasStoredTokens()) {
    return router.createUrlTree(['/login']);
  }

  return auth.restoreSession().pipe(
    map((user) => (user ? true : router.createUrlTree(['/login']))),
  );
};

export const guestGuard: CanActivateFn = () => {
  const session = inject(UserSessionService);
  const router = inject(Router);

  if (session.isAuthenticated()) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
