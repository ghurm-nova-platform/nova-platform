import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { IdentityConfigResponse, MfaEnrollResponse } from '../identity.models';
import { extractErrorMessage, passwordPolicySummary } from '../identity.utils';

@Component({
  selector: 'app-identity-configuration-page',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './identity-configuration-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityConfigurationPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly config = signal<IdentityConfigResponse | null>(null);
  readonly enrollment = signal<MfaEnrollResponse | null>(null);
  readonly verifyMessage = signal<string | null>(null);

  mfaVerifyCode = '';
  selectedMfaMethod: 'TOTP' = 'TOTP';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getConfig().subscribe({
      next: (cfg) => {
        this.config.set(cfg);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load identity configuration'));
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
      error: (err) => this.error.set(extractErrorMessage(err, 'Failed to start MFA enrollment')),
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
        next: () => {
          this.enrollment.set(null);
          this.mfaVerifyCode = '';
          this.verifyMessage.set('MFA enrollment verified successfully.');
        },
        error: (err) => this.error.set(extractErrorMessage(err, 'Failed to verify MFA enrollment')),
      });
  }

  describePasswordPolicy(config: IdentityConfigResponse): string {
    return passwordPolicySummary(config);
  }
}
