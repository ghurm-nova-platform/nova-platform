import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { RuntimeConfigService } from '../config/runtime-config.service';

/**
 * Adds the runtime-configured API key when present.
 * Never hardcodes credentials.
 */
export const apiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  const runtimeConfig = inject(RuntimeConfigService);
  const apiKey = runtimeConfig.apiKey().trim();
  if (!apiKey) {
    return next(req);
  }
  return next(
    req.clone({
      setHeaders: {
        'X-API-Key': apiKey,
      },
    }),
  );
};
