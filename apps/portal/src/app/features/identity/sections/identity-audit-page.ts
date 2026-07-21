import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { AuditService } from '../../audit/audit.service';
import { AuditEvent } from '../../audit/audit.models';
import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { LoginHistoryEntry } from '../identity.models';
import { extractErrorMessage, outcomeClass } from '../identity.utils';

@Component({
  selector: 'app-identity-audit-page',
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './identity-audit-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityAuditPage implements OnInit {
  private readonly auditApi = inject(AuditService);
  private readonly identityApi = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<AuditEvent[]>([]);
  readonly loginHistory = signal<LoginHistoryEntry[]>([]);
  readonly fallbackNotice = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    if (!this.permissions.canReadAudit()) {
      this.error.set('You need IDENTITY_AUDIT_READ or AUDIT_READ permission.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.fallbackNotice.set(null);
    this.auditApi.search({ entityType: 'USER' as AuditEvent['entityType'], page: 0, size: 50 }).pipe(
      catchError(() => this.auditApi.list({ page: 0, size: 50 })),
      catchError(() => of(null)),
    ).subscribe({
      next: (response) => {
        if (response?.events?.length) {
          const identityEvents = response.events.filter(
            (event) =>
              event.source === 'PORTAL' ||
              event.action === 'LOGIN' ||
              event.action === 'LOGOUT' ||
              event.action === 'ACCESS',
          );
          this.events.set(identityEvents.length > 0 ? identityEvents : response.events);
          this.loading.set(false);
          return;
        }
        this.loadLoginHistoryFallback();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load audit events'));
      },
    });
  }

  private loadLoginHistoryFallback(): void {
    this.identityApi.loginHistory().subscribe({
      next: (entries) => {
        this.loginHistory.set(entries);
        this.fallbackNotice.set(
          'Audit API unavailable or returned no identity events — showing login history instead. You can also open the full Audit Center.',
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load identity audit data'));
      },
    });
  }

  outcomeClass(outcome: string): string {
    return outcomeClass(outcome);
  }
}
