import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ModelProvider } from './model-gateway.models';
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
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly providerId = signal('');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly provider = signal<ModelProvider | null>(null);

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

  statusClass(status: ModelProvider['status']): string {
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
