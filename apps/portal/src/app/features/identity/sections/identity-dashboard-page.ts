import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { IdentityService } from '../identity.service';
import {
  IdentityDashboardStats,
  IdentitySessionView,
  LoginHistoryEntry,
  SecurityEventView,
} from '../identity.models';
import { activeSessionCount, extractErrorMessage, outcomeClass } from '../identity.utils';

@Component({
  selector: 'app-identity-dashboard-page',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './identity-dashboard-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityDashboardPage implements OnInit {
  private readonly api = inject(IdentityService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly stats = signal<IdentityDashboardStats | null>(null);
  readonly fallbackNotice = signal<string | null>(null);

  readonly recentLogins = computed(() => this.stats()?.recentLogins ?? []);
  readonly securityAlerts = computed(() => this.stats()?.securityAlerts ?? []);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.fallbackNotice.set(null);
    this.api.getDashboard().pipe(catchError(() => of(null))).subscribe({
      next: (dashboard) => {
        if (dashboard) {
          this.stats.set(dashboard);
          this.loading.set(false);
          return;
        }
        this.loadFallbackDashboard();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load identity dashboard'));
      },
    });
  }

  private loadFallbackDashboard(): void {
    forkJoin({
      summary: this.api.getSummary().pipe(catchError(() => of(null))),
      sessions: this.api.listSessions().pipe(catchError(() => of([] as IdentitySessionView[]))),
      loginHistory: this.api.loginHistory().pipe(catchError(() => of([] as LoginHistoryEntry[]))),
      providers: this.api.listProviders().pipe(catchError(() => of([]))),
      securityEvents: this.api
        .listSecurityEvents()
        .pipe(catchError(() => of([] as SecurityEventView[]))),
    }).subscribe({
      next: ({ summary, sessions, loginHistory, providers, securityEvents }) => {
        const users = summary?.users ?? [];
        const failedLogins = loginHistory.filter((entry) => entry.outcome === 'FAILURE');
        const lockedAccounts = users.filter((user) => user.locked || user.status === 'LOCKED').length;
        const mfaEnabled = users.filter((user) => user.mfaEnabled).length;
        const mfaAdoptionPercent =
          users.length > 0 ? Math.round((mfaEnabled / users.length) * 100) : 0;

        this.stats.set({
          activeUsers: users.filter((user) => user.status === 'ACTIVE').length,
          onlineSessions: activeSessionCount(sessions),
          failedLogins24h: failedLogins.length,
          lockedAccounts,
          mfaAdoptionPercent,
          providerCount: providers.length,
          recentLogins: loginHistory.slice(0, 10),
          securityAlerts:
            securityEvents.length > 0
              ? securityEvents.slice(0, 5)
              : failedLogins.slice(0, 5).map((entry) => ({
                  id: entry.id,
                  type: 'LOGIN_FAILURE' as const,
                  userId: entry.userId,
                  userEmail: entry.userEmail,
                  ipAddress: entry.ipAddress,
                  userAgent: entry.userAgent,
                  message: entry.failureReason ?? 'Login failed',
                  severity: 'MEDIUM' as const,
                  occurredAt: entry.occurredAt,
                })),
        });
        this.fallbackNotice.set(
          'Dashboard API unavailable — showing aggregated data from summary, sessions, and login history.',
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load identity dashboard'));
      },
    });
  }

  outcomeClass(outcome: string): string {
    return outcomeClass(outcome);
  }
}
