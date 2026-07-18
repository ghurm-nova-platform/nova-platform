import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AI_PROVIDER_TYPES, AiProviderType, ModelProvider } from './model-gateway.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelProviderService } from './model-provider.service';

@Component({
  selector: 'app-model-provider-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './model-provider-form-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ModelProviderFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly providersApi = inject(ModelProviderService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly providerId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly adapterKeys = signal<string[]>([]);
  readonly currentProvider = signal<ModelProvider | null>(null);
  readonly types = AI_PROVIDER_TYPES;

  readonly form = this.fb.nonNullable.group({
    providerKey: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/), Validators.maxLength(100)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    providerType: ['DETERMINISTIC_LOCAL' as AiProviderType, Validators.required],
    adapterKey: ['', Validators.required],
    credentialReference: [''],
    region: ['', Validators.maxLength(100)],
    requestTimeoutSeconds: [60, [Validators.required, Validators.min(1), Validators.max(300)]],
    maxConcurrentRequests: [10, [Validators.required, Validators.min(1), Validators.max(1000)]],
    maxRetries: [1, [Validators.required, Validators.min(0), Validators.max(5)]],
    retryBackoffMs: [250, [Validators.required, Validators.min(0), Validators.max(10000)]],
  });

  ngOnInit(): void {
    const providerId = this.route.snapshot.paramMap.get('providerId');
    this.providerId.set(providerId);
    this.editMode.set(!!providerId && this.route.snapshot.url.some((segment) => segment.path === 'edit'));

    if (providerId) {
      if (!this.permissions.canUpdateProvider()) {
        this.unauthorized.set(true);
        return;
      }
      this.form.controls.providerKey.disable();
      this.form.controls.providerType.disable();
      this.form.controls.adapterKey.disable();
      this.loadProvider(providerId);
    } else if (!this.permissions.canCreateProvider()) {
      this.unauthorized.set(true);
    }

    this.loadAdapters();
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    const credentialReference = raw.credentialReference.trim() || null;
    const region = raw.region.trim() || null;

    if (this.editMode() && this.providerId()) {
      const current = this.currentProvider();
      if (!current) {
        return;
      }
      this.providersApi
        .updateProvider(this.providerId()!, {
          version: current.version,
          name: raw.name.trim(),
          description: raw.description.trim() || null,
          credentialReference,
          region,
          requestTimeoutSeconds: raw.requestTimeoutSeconds,
          maxConcurrentRequests: raw.maxConcurrentRequests,
          maxRetries: raw.maxRetries,
          retryBackoffMs: raw.retryBackoffMs,
        })
        .subscribe({
          next: (provider) => {
            this.saving.set(false);
            this.currentProvider.set(provider);
            void this.router.navigate(['/model-providers', provider.id]);
          },
          error: (err: { status?: number; error?: { message?: string } }) => {
            this.saving.set(false);
            if (err.status === 403) {
              this.unauthorized.set(true);
              return;
            }
            this.error.set(err.error?.message ?? 'Unable to save provider.');
          },
        });
      return;
    }

    this.providersApi
      .createProvider({
        providerKey: raw.providerKey.trim(),
        name: raw.name.trim(),
        description: raw.description.trim() || null,
        providerType: raw.providerType,
        adapterKey: raw.adapterKey,
        credentialReference,
        region,
        requestTimeoutSeconds: raw.requestTimeoutSeconds,
        maxConcurrentRequests: raw.maxConcurrentRequests,
        maxRetries: raw.maxRetries,
        retryBackoffMs: raw.retryBackoffMs,
      })
      .subscribe({
        next: (provider) => {
          this.saving.set(false);
          void this.router.navigate(['/model-providers', provider.id]);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create provider.');
        },
      });
  }

  activate(): void {
    const current = this.currentProvider();
    if (!current || !this.permissions.canActivateProvider() || !window.confirm(`Activate "${current.name}"?`)) {
      return;
    }
    this.providersApi.activateProvider(current.id).subscribe({
      next: (provider) => this.currentProvider.set(provider),
      error: () => this.error.set('Unable to activate provider.'),
    });
  }

  disable(): void {
    const current = this.currentProvider();
    if (!current || !this.permissions.canDisableProvider() || !window.confirm(`Disable "${current.name}"?`)) {
      return;
    }
    this.providersApi.disableProvider(current.id).subscribe({
      next: (provider) => this.currentProvider.set(provider),
      error: () => this.error.set('Unable to disable provider.'),
    });
  }

  archive(): void {
    const current = this.currentProvider();
    if (!current || !this.permissions.canArchiveProvider() || !window.confirm(`Archive "${current.name}"?`)) {
      return;
    }
    this.providersApi.archiveProvider(current.id).subscribe({
      next: () => void this.router.navigate(['/model-providers']),
      error: () => this.error.set('Unable to archive provider.'),
    });
  }

  statusClass(status: ModelProvider['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadProvider(providerId: string): void {
    this.loading.set(true);
    this.providersApi.getProvider(providerId).subscribe({
      next: (provider) => {
        this.currentProvider.set(provider);
        this.form.patchValue({
          providerKey: provider.providerKey,
          name: provider.name,
          description: provider.description ?? '',
          providerType: provider.providerType,
          adapterKey: provider.adapterKey,
          credentialReference: provider.credentialReference ?? '',
          region: provider.region ?? '',
          requestTimeoutSeconds: provider.requestTimeoutSeconds,
          maxConcurrentRequests: provider.maxConcurrentRequests,
          maxRetries: provider.maxRetries,
          retryBackoffMs: provider.retryBackoffMs,
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load provider.');
        this.loading.set(false);
      },
    });
  }

  private loadAdapters(): void {
    if (!this.permissions.canReadProviders()) {
      return;
    }
    this.providersApi.listAdapters().subscribe({
      next: (response) => this.adapterKeys.set(response.adapterKeys),
      error: () => this.error.set('Unable to load adapter allowlist.'),
    });
  }
}
