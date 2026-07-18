import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AiProviderType, PROVIDER_SECRET_TYPES } from './model-gateway.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ProviderSecretService } from './provider-secret.service';

@Component({
  selector: 'app-provider-secret-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './provider-secret-form-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ProviderSecretFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly secretsApi = inject(ProviderSecretService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly createdCredentialReference = signal<string | null>(null);
  readonly copyFeedback = signal<string | null>(null);
  readonly types = PROVIDER_SECRET_TYPES;

  readonly form = this.fb.nonNullable.group({
    secretKey: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/), Validators.maxLength(100)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    providerType: ['OPENAI' as AiProviderType, Validators.required],
    secret: ['', [Validators.required, Validators.minLength(1)]],
  });

  ngOnInit(): void {
    if (!this.permissions.canCreateProviderSecret()) {
      this.unauthorized.set(true);
    }
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.createdCredentialReference.set(null);
    this.copyFeedback.set(null);
    const raw = this.form.getRawValue();

    this.secretsApi
      .createSecret({
        secretKey: raw.secretKey.trim(),
        name: raw.name.trim(),
        description: raw.description.trim() || null,
        providerType: raw.providerType,
        secret: raw.secret,
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this.form.reset({
            secretKey: '',
            name: '',
            description: '',
            providerType: 'OPENAI',
            secret: '',
          });
          this.createdCredentialReference.set(created.credentialReference);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create provider secret.');
        },
      });
  }

  async copyReference(): Promise<void> {
    const reference = this.createdCredentialReference();
    if (!reference) {
      return;
    }
    try {
      await navigator.clipboard.writeText(reference);
      this.copyFeedback.set('Credential reference copied.');
    } catch {
      this.copyFeedback.set('Unable to copy. Select the reference manually.');
    }
  }

  openCreated(): void {
    const reference = this.createdCredentialReference();
    if (!reference) {
      return;
    }
    const id = reference.replace(/^vault:provider-secret:/, '');
    void this.router.navigate(['/provider-secrets', id]);
  }
}
