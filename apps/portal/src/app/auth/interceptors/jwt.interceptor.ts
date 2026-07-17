import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { UserSessionService } from '../services/user-session.service';

/**
 * Attaches the Platform API Bearer access token.
 * Never attaches Agent Runtime credentials.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(UserSessionService);
  const token = session.getAccessToken();

  if (!token || req.headers.has('Authorization')) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
