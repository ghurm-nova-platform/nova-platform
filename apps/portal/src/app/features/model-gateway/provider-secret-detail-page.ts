import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ProviderSecret } from './model-gateway.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ProviderSecretService } from './provider-secret.service';

@Component({
  selector: 'app-provider-secret-detail-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './provider-secret-detail-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ProviderSecretDetailPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly secretsApi = inject(ProviderSecretService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly secretId = signal('');
  readonly loading = signal(true);
  readonly rotating = signal(false);
  readonly revoking = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly secret = signal<ProviderSecret | null>(null);
  readonly copyFeedback = signal<string | null>(null);

  readonly rotateForm = this.fb.nonNullable.group({
    secret: ['', [Validators.required, Validators.minLength(1)]],
  });

  ngOnInit(): void {
    this.secretId.set(this.route.snapshot.paramMap.get('secretId') ?? '');
    this.load();
  }

  async copyReference(): Promise<void> {
    const current = this.secret();
    if (!current) {
      return;
    }
    try {
      await navigator.clipboard.writeText(current.credentialReference);
      this.copyFeedback.set('Credential reference copied.');
    } catch {
      this.copyFeedback.set('Unable to copy. Select the reference manually.');
    }
  }

  rotate(): void {
    const current = this.secret();
    if (
      !current ||
      current.status !== 'ACTIVE' ||
      !this.permissions.canRotateProviderSecret() ||
      this.rotateForm.invalid ||
      this.rotating()
    ) {
      this.rotateForm.markAllAsTouched();
      return;
    }
    if (!window.confirm(`Rotate secret "${current.name}"? The previous value cannot be recovered.`)) {
      return;
    }

    this.rotating.set(true);
    this.error.set(null);
    const plaintext = this.rotateForm.controls.secret.value;
    this.secretsApi.rotateSecret(current.id, { secret: plaintext }).subscribe({
      next: (updated) => {
        this.rotating.set(false);
        this.secret.set(updated);
        this.rotateForm.reset({ secret: '' });
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.rotating.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to rotate secret.');
      },
    });
  }

  revoke(): void {
    const current = this.secret();
    if (
      !current ||
      current.status === 'REVOKED' ||
      !this.permissions.canRevokeProviderSecret() ||
      this.revoking() ||
      !window.confirm(`Revoke secret "${current.name}"? Providers using this reference will fail credential resolution.`)
    ) {
      return;
    }

    this.revoking.set(true);
    this.error.set(null);
    this.secretsApi.revokeSecret(current.id).subscribe({
      next: (updated) => {
        this.revoking.set(false);
        this.secret.set(updated);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.revoking.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to revoke secret.');
      },
    });
  }

  statusClass(status: ProviderSecret['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private load(): void {
    if (!this.permissions.canReadProviderSecrets()) {
      this.unauthorized.set(true);
      this.loading.set(false);
      return;
    }
    this.secretsApi.getSecret(this.secretId()).subscribe({
      next: (secret) => {
        this.secret.set(secret);
        this.loading.set(false);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load provider secret.');
      },
    });
  }
}
