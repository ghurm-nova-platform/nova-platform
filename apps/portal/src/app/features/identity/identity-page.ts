import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';

import { IdentityPermissionHelper } from './identity-permission.helper';
import { IdentityService } from './identity.service';
import {
  IdentityConfigResponse,
  IdentityProviderView,
  IdentitySessionView,
  LoginHistoryEntry,
  MfaEnrollResponse,
  MfaStatusResponse,
  ScimUserSummary,
} from './identity.models';

@Component({
  selector: 'app-identity-page',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTabsModule,
  ],
  templateUrl: './identity-page.html',
  styleUrl: './identity-page.scss',
})
export class IdentityPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly config = signal<IdentityConfigResponse | null>(null);
  readonly providers = signal<IdentityProviderView[]>([]);
  readonly sessions = signal<IdentitySessionView[]>([]);
  readonly loginHistory = signal<LoginHistoryEntry[]>([]);
  readonly mfaStatus = signal<MfaStatusResponse | null>(null);
  readonly scimUsers = signal<ScimUserSummary[]>([]);
  readonly enrollment = signal<MfaEnrollResponse | null>(null);
  readonly verifyMessage = signal<string | null>(null);

  mfaVerifyCode = '';
  selectedMfaMethod: 'TOTP' | 'WEBAUTHN' = 'TOTP';

  readonly activeProviders = computed(() =>
    this.providers().filter((provider) => provider.status === 'ACTIVE'),
  );

  readonly activeSessions = computed(() =>
    this.sessions().filter((session) => session.status === 'ACTIVE'),
  );

  readonly failedLogins = computed(() =>
    this.loginHistory().filter((entry) => entry.outcome === 'FAILURE'),
  );

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      return;
    }
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getConfig().subscribe({
      next: (config) => {
        this.config.set(config);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load identity configuration');
      },
    });
    this.loadProviders();
    this.loadSessions();
    this.loadLoginHistory();
    this.loadMfaStatus();
    if (this.permissions.canProvisionScim()) {
      this.loadScimUsers();
    }
  }

  loadProviders(): void {
    this.api.listProviders().subscribe({
      next: (items) => this.providers.set(items),
      error: () => this.providers.set([]),
    });
  }

  loadSessions(): void {
    this.api.listSessions().subscribe({
      next: (items) => this.sessions.set(items),
      error: () => this.sessions.set([]),
    });
  }

  loadLoginHistory(): void {
    this.api.loginHistory().subscribe({
      next: (items) => this.loginHistory.set(items),
      error: () => this.loginHistory.set([]),
    });
  }

  loadMfaStatus(): void {
    this.api.mfaStatus().subscribe({
      next: (status) => this.mfaStatus.set(status),
      error: () => this.mfaStatus.set(null),
    });
  }

  loadScimUsers(): void {
    this.api.listScimUsers().subscribe({
      next: (items) => this.scimUsers.set(items),
      error: () => this.scimUsers.set([]),
    });
  }

  revokeSession(id: string): void {
    if (!this.permissions.canAdmin()) {
      return;
    }
    this.loading.set(true);
    this.api.revokeSession(id).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadSessions();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to revoke session');
      },
    });
  }

  startMfaEnrollment(): void {
    if (!this.permissions.canManageMfa()) {
      return;
    }
    this.verifyMessage.set(null);
    this.api.mfaEnroll({ method: this.selectedMfaMethod }).subscribe({
      next: (response) => {
        this.enrollment.set(response);
        this.verifyMessage.set('Scan the QR code or enter the secret, then verify with a code.');
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Failed to start MFA enrollment'),
    });
  }

  verifyMfaEnrollment(): void {
    const enrollment = this.enrollment();
    if (!enrollment || !this.mfaVerifyCode.trim()) {
      return;
    }
    this.api
      .mfaVerifyEnrollment({
        enrollmentToken: enrollment.enrollmentToken,
        code: this.mfaVerifyCode.trim(),
      })
      .subscribe({
        next: (response) => {
          this.enrollment.set(null);
          this.mfaVerifyCode = '';
          this.mfaStatus.set({
            ...this.mfaStatus()!,
            status: response.status,
            enrolledMethods: [...(this.mfaStatus()?.enrolledMethods ?? []), enrollment.method],
          });
          this.verifyMessage.set('MFA enrollment verified successfully.');
          this.loadMfaStatus();
        },
        error: (err) => this.error.set(err?.error?.message ?? 'Failed to verify MFA enrollment'),
      });
  }

  providerTypeIcon(type: string): string {
    switch (type) {
      case 'SAML':
        return 'security';
      case 'OIDC':
        return 'key';
      case 'LDAP':
        return 'folder_shared';
      default:
        return 'person';
    }
  }

  outcomeClass(outcome: string): string {
    return `identity__outcome identity__outcome--${outcome.toLowerCase()}`;
  }

  describePasswordPolicy(config: IdentityConfigResponse): string {
    return passwordPolicySummary(config);
  }
}

export function activeSessionCount(sessions: IdentitySessionView[]): number {
  return sessions.filter((session) => session.status === 'ACTIVE').length;
}

export function passwordPolicySummary(config: IdentityConfigResponse): string {
  const rules: string[] = [`min ${config.passwordMinLength} chars`];
  if (config.passwordRequireUppercase) {
    rules.push('uppercase');
  }
  if (config.passwordRequireLowercase) {
    rules.push('lowercase');
  }
  if (config.passwordRequireDigit) {
    rules.push('digit');
  }
  if (config.passwordRequireSpecial) {
    rules.push('special');
  }
  if (config.passwordMaxAgeDays > 0) {
    rules.push(`max age ${config.passwordMaxAgeDays} days`);
  }
  return rules.join(', ');
}
