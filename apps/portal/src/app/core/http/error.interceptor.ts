import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/**
 * Normalizes HTTP failures into a consistent client-side error channel.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const message =
          typeof error.error === 'object' &&
          error.error !== null &&
          'message' in error.error &&
          typeof (error.error as { message: unknown }).message === 'string'
            ? (error.error as { message: string }).message
            : error.message;
        return throwError(
          () =>
            new Error(
              `HTTP ${error.status} ${error.statusText || 'Error'} for ${req.method} ${req.url}: ${message}`,
            ),
        );
      }
      return throwError(() => error);
    }),
  );
};
