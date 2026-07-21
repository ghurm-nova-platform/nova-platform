import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { catchError, of } from 'rxjs';

import { IdentityService } from '../identity.service';
import { LoginHistoryEntry, SecurityEventView } from '../identity.models';
import { extractErrorMessage, outcomeClass } from '../identity.utils';

@Component({
  selector: 'app-identity-security-events-page',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './identity-security-events-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentitySecurityEventsPage implements OnInit {
  private readonly api = inject(IdentityService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<SecurityEventView[]>([]);
  readonly fallbackNotice = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.fallbackNotice.set(null);
    this.api.listSecurityEvents().pipe(catchError(() => of(null))).subscribe({
      next: (items) => {
        if (items && items.length > 0) {
          this.events.set(items);
          this.loading.set(false);
          return;
        }
        this.loadLoginHistoryFallback();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load security events'));
      },
    });
  }

  private loadLoginHistoryFallback(): void {
    this.api.loginHistory().subscribe({
      next: (entries) => {
        this.events.set(entries.map((entry) => toSecurityEvent(entry)));
        this.fallbackNotice.set(
          'Security events API unavailable — showing login history as security events.',
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load security events'));
      },
    });
  }

  outcomeClass(outcome: string): string {
    return outcomeClass(outcome);
  }
}

function toSecurityEvent(entry: LoginHistoryEntry): SecurityEventView {
  return {
    id: entry.id,
    type: entry.outcome === 'SUCCESS' ? 'LOGIN_SUCCESS' : 'LOGIN_FAILURE',
    userId: entry.userId,
    userEmail: entry.userEmail,
    ipAddress: entry.ipAddress,
    userAgent: entry.userAgent,
    message: entry.failureReason ?? `${entry.outcome} via ${entry.authMethod}`,
    severity: entry.outcome === 'FAILURE' ? 'MEDIUM' : 'LOW',
    occurredAt: entry.occurredAt,
  };
}
