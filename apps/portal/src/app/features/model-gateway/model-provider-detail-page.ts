import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ConnectionTestResponse, ConnectionTestStatus, CatalogSyncResult, ModelProvider } from './model-gateway.models';
import { CatalogModelService } from './catalog-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelProviderService } from './model-provider.service';

@Component({
  selector: 'app-model-provider-detail-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './model-provider-detail-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ModelProviderDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly providersApi = inject(ModelProviderService);
  private readonly catalogApi = inject(CatalogModelService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly providerId = signal('');
  readonly loading = signal(true);
  readonly testing = signal(false);
  readonly syncing = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly provider = signal<ModelProvider | null>(null);
  readonly lastTest = signal<ConnectionTestResponse | null>(null);
  readonly lastSync = signal<CatalogSyncResult | null>(null);

  ngOnInit(): void {
    this.providerId.set(this.route.snapshot.paramMap.get('providerId') ?? '');
    this.load();
  }

  edit(): void {
    void this.router.navigate(['/model-providers', this.providerId(), 'edit']);
  }

  openModels(): void {
    void this.router.navigate(['/model-providers', this.providerId(), 'models']);
  }

  activate(): void {
    const current = this.provider();
    if (!current || !this.permissions.canActivateProvider() || !window.confirm(`Activate "${current.name}"?`)) {
      return;
    }
    this.providersApi.activateProvider(current.id).subscribe({
      next: (provider) => this.provider.set(provider),
      error: () => this.error.set('Unable to activate provider.'),
    });
  }

  disable(): void {
    const current = this.provider();
    if (!current || !this.permissions.canDisableProvider() || !window.confirm(`Disable "${current.name}"?`)) {
      return;
    }
    this.providersApi.disableProvider(current.id).subscribe({
      next: (provider) => this.provider.set(provider),
      error: () => this.error.set('Unable to disable provider.'),
    });
  }

  archive(): void {
    const current = this.provider();
    if (!current || !this.permissions.canArchiveProvider() || !window.confirm(`Archive "${current.name}"?`)) {
      return;
    }
    this.providersApi.archiveProvider(current.id).subscribe({
      next: () => void this.router.navigate(['/model-providers']),
      error: () => this.error.set('Unable to archive provider.'),
    });
  }

  testConnection(): void {
    const current = this.provider();
    if (!current || !this.permissions.canTestProviderConnection() || this.testing()) {
      return;
    }
    this.testing.set(true);
    this.error.set(null);
    this.providersApi.testConnection(current.id).subscribe({
      next: (result) => {
        this.testing.set(false);
        this.lastTest.set(result);
        this.provider.update((provider) =>
          provider
            ? {
                ...provider,
                lastConnectionTestStatus: result.status,
                lastConnectionTestAt: result.testedAt,
                lastConnectionTestErrorCode: result.errorCode,
              }
            : provider,
        );
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.testing.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to test provider connection.');
      },
    });
  }

  canSyncModels(): boolean {
    const current = this.provider();
    return (
      !!current &&
      current.status === 'ACTIVE' &&
      current.lastConnectionTestStatus === 'SUCCESS' &&
      this.permissions.canSyncCatalog()
    );
  }

  syncModels(): void {
    const current = this.provider();
    if (!current || !this.canSyncModels() || this.syncing()) {
      return;
    }
    this.syncing.set(true);
    this.error.set(null);
    this.catalogApi.syncModels(current.id).subscribe({
      next: (result) => {
        this.syncing.set(false);
        this.lastSync.set(result);
        this.provider.update((provider) =>
          provider
            ? {
                ...provider,
                lastModelSyncAt: result.syncedAt,
                lastModelSyncStatus: result.status,
                lastModelSyncErrorCode: result.errorCode,
                lastModelSyncDiscoveredCount: result.discoveredCount,
                lastModelSyncCreatedCount: result.createdCount,
                lastModelSyncUpdatedCount: result.updatedCount,
                lastModelSyncUnchangedCount: result.unchangedCount,
              }
            : provider,
        );
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.syncing.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to sync provider models.');
      },
    });
  }

  syncSummary(provider: ModelProvider, sync: CatalogSyncResult | null = null): string {
    const status = sync?.status ?? provider.lastModelSyncStatus;
    if (!status) {
      return 'Never synced';
    }
    const discovered = sync?.discoveredCount ?? provider.lastModelSyncDiscoveredCount ?? 0;
    const created = sync?.createdCount ?? provider.lastModelSyncCreatedCount ?? 0;
    const updated = sync?.updatedCount ?? provider.lastModelSyncUpdatedCount ?? 0;
    const unchanged = sync?.unchangedCount ?? provider.lastModelSyncUnchangedCount ?? 0;
    const errorCode = sync?.errorCode ?? provider.lastModelSyncErrorCode;
    const base = `${status}: discovered ${discovered}, created ${created}, updated ${updated}, unchanged ${unchanged}`;
    return errorCode ? `${base} · ${errorCode}` : base;
  }

  statusClass(status: ModelProvider['status'] | ConnectionTestStatus | string): string {
    return `status status--${status.toLowerCase()}`;
  }

  private load(): void {
    if (!this.permissions.canReadProviders()) {
      this.unauthorized.set(true);
      this.loading.set(false);
      return;
    }
    this.providersApi.getProvider(this.providerId()).subscribe({
      next: (provider) => {
        this.provider.set(provider);
        this.loading.set(false);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load provider.');
      },
    });
  }
}
