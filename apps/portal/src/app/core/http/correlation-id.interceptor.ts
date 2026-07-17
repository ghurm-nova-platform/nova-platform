import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Attaches a correlation ID to every outbound HTTP request.
 */
export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = crypto.randomUUID();
  return next(
    req.clone({
      setHeaders: {
        'X-Correlation-Id': correlationId,
      },
    }),
  );
};
