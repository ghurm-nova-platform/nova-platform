import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { extractErrorMessage } from '../identity.utils';

@Component({
  selector: 'app-identity-sessions-page',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './identity-sessions-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentitySessionsPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly sessions = signal(
    [] as import('../identity.models').IdentitySessionView[],
  );

  readonly activeSessions = computed(() =>
    this.sessions().filter((session) => session.status === 'ACTIVE'),
  );

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listSessions().subscribe({
      next: (items) => {
        this.sessions.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load sessions'));
        this.sessions.set([]);
      },
    });
  }

  revokeSession(id: string): void {
    if (!this.permissions.canManageSessions()) {
      return;
    }
    this.loading.set(true);
    this.api.revokeSession(id).subscribe({
      next: () => {
        this.message.set('Session revoked.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to revoke session'));
      },
    });
  }

  revokeAll(): void {
    if (!this.permissions.canManageSessions()) {
      return;
    }
    this.loading.set(true);
    this.api.revokeAllSessions().subscribe({
      next: () => {
        this.message.set('All sessions revoked.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to revoke all sessions'));
      },
    });
  }
}
