import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap, throwError } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { AuthUser, LoginRequest, TokenResponse } from './auth.models';
import { UserSessionService } from './user-session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiClient);
  private readonly session = inject(UserSessionService);

  login(request: LoginRequest): Observable<AuthUser> {
    return this.api.post<TokenResponse>('/api/auth/login', request).pipe(
      switchMap((tokens) => this.persistAndLoadMe(tokens)),
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.session.getRefreshToken();
    const clearLocal = () => {
      this.session.clear();
    };

    if (!refreshToken) {
      clearLocal();
      return of(void 0);
    }

    return this.api.post<void>('/api/auth/logout', { refreshToken }).pipe(
      tap({ next: clearLocal, error: clearLocal }),
      map(() => void 0),
      catchError(() => {
        clearLocal();
        return of(void 0);
      }),
    );
  }

  restoreSession(): Observable<AuthUser | null> {
    if (!this.session.hasStoredTokens()) {
      return of(null);
    }

    return this.api.get<AuthUser>('/api/auth/me').pipe(
      tap((user) => this.session.setUser(user)),
      catchError(() => {
        const refreshToken = this.session.getRefreshToken();
        if (!refreshToken) {
          this.session.clear();
          return of(null);
        }
        return this.api.post<TokenResponse>('/api/auth/refresh', { refreshToken }).pipe(
          switchMap((tokens) => this.persistAndLoadMe(tokens)),
          catchError(() => {
            this.session.clear();
            return of(null);
          }),
        );
      }),
    );
  }

  me(): Observable<AuthUser> {
    return this.api.get<AuthUser>('/api/auth/me');
  }

  private persistAndLoadMe(tokens: TokenResponse): Observable<AuthUser> {
    this.session.updateTokens({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
    });
    return this.api.get<AuthUser>('/api/auth/me').pipe(
      tap((user) => this.session.setSession(
        { accessToken: tokens.accessToken, refreshToken: tokens.refreshToken },
        user,
      )),
      catchError((err) => {
        this.session.clear();
        return throwError(() => err);
      }),
    );
  }
}
