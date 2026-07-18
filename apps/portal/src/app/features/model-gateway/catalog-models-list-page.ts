import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { CatalogModelService } from './catalog-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelProviderService } from './model-provider.service';
import {
  AI_MODEL_CAPABILITIES,
  AI_MODEL_SOURCES,
  AI_MODEL_STATUSES,
  AiModelCapability,
  AiModelSource,
  AiModelStatus,
  CatalogModel,
  ModelProvider,
} from './model-gateway.models';

@Component({
  selector: 'app-catalog-models-list-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './catalog-models-list-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class CatalogModelsListPage implements OnInit {
  private readonly router = inject(Router);
  private readonly catalogApi = inject(CatalogModelService);
  private readonly providersApi = inject(ModelProviderService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<AiModelStatus | ''>('', { nonNullable: true });
  readonly sourceControl = new FormControl<AiModelSource | ''>('', { nonNullable: true });
  readonly capabilityControl = new FormControl<AiModelCapability | ''>('', { nonNullable: true });
  readonly providerControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = [
    'modelKey',
    'providerName',
    'providerModelId',
    'status',
    'contextWindowTokens',
    'capabilities',
    'lastSyncedAt',
    'actions',
  ];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<CatalogModel[]>([]);
  readonly providers = signal<ModelProvider[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = AI_MODEL_STATUSES;
  readonly sources = AI_MODEL_SOURCES;
  readonly capabilities = AI_MODEL_CAPABILITIES;

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.statusControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.sourceControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.capabilityControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.providerControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.loadProviders();
    this.load();
  }

  load(): void {
    if (!this.permissions.canReadCatalog()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.catalogApi
      .list({
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        source: this.sourceControl.value || undefined,
        capability: this.capabilityControl.value || undefined,
        providerId: this.providerControl.value || undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .subscribe({
        next: (page) => {
          this.rows.set(page.content);
          this.total.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err: { status?: number }) => {
          this.loading.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set('Unable to load AI models.');
        },
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  onSort(sort: Sort): void {
    this.sort.set(!sort.active || !sort.direction ? 'createdAt,desc' : `${sort.active},${sort.direction}`);
    this.load();
  }

  create(): void {
    void this.router.navigate(['/ai-models', 'new']);
  }

  open(model: CatalogModel): void {
    void this.router.navigate(['/ai-models', model.id]);
  }

  edit(model: CatalogModel): void {
    void this.router.navigate(['/ai-models', model.id, 'edit']);
  }

  capabilitiesSummary(model: CatalogModel): string {
    const enabled = (model.capabilities ?? [])
      .filter((cap) => cap.enabled)
      .map((cap) => cap.capability);
    if (enabled.length === 0) {
      return '—';
    }
    if (enabled.length <= 3) {
      return enabled.join(', ');
    }
    return `${enabled.slice(0, 3).join(', ')} +${enabled.length - 3}`;
  }

  statusClass(status: AiModelStatus): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadProviders(): void {
    if (!this.permissions.canReadProviders()) {
      return;
    }
    this.providersApi.listProviders({ page: 0, size: 100, sort: 'name,asc' }).subscribe({
      next: (page) => this.providers.set(page.content),
      error: () => this.providers.set([]),
    });
  }
}
