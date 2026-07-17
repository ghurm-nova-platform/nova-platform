import { Injectable, computed, signal } from '@angular/core';

import { AuthUser } from './auth.models';

const ACCESS_TOKEN_KEY = 'nova.accessToken';
const REFRESH_TOKEN_KEY = 'nova.refreshToken';

/**
 * In-memory + sessionStorage session state.
 * Passwords are never stored. Tokens stay in sessionStorage (tab-scoped).
 */
@Injectable({ providedIn: 'root' })
export class UserSessionService {
  private readonly userSignal = signal<AuthUser | null>(null);
  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.accessToken && !!this.userSignal());

  constructor() {
    this.accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY);
    this.refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getRefreshToken(): string | null {
    return this.refreshToken;
  }

  setSession(tokens: { accessToken: string; refreshToken: string }, user: AuthUser): void {
    this.accessToken = tokens.accessToken;
    this.refreshToken = tokens.refreshToken;
    this.userSignal.set(user);
    sessionStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  }

  updateTokens(tokens: { accessToken: string; refreshToken: string }): void {
    this.accessToken = tokens.accessToken;
    this.refreshToken = tokens.refreshToken;
    sessionStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  }

  setUser(user: AuthUser): void {
    this.userSignal.set(user);
  }

  clear(): void {
    this.accessToken = null;
    this.refreshToken = null;
    this.userSignal.set(null);
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  hasStoredTokens(): boolean {
    return !!this.accessToken;
  }
}
