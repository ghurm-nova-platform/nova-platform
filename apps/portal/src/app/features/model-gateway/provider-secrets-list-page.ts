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

import {
  AiProviderType,
  PROVIDER_SECRET_STATUSES,
  PROVIDER_SECRET_TYPES,
  ProviderSecret,
  ProviderSecretStatus,
} from './model-gateway.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ProviderSecretService } from './provider-secret.service';

@Component({
  selector: 'app-provider-secrets-list-page',
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
  templateUrl: './provider-secrets-list-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ProviderSecretsListPage implements OnInit {
  private readonly router = inject(Router);
  private readonly secretsApi = inject(ProviderSecretService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<ProviderSecretStatus | ''>('', { nonNullable: true });
  readonly typeControl = new FormControl<AiProviderType | ''>('', { nonNullable: true });
  readonly displayedColumns = [
    'name',
    'secretKey',
    'status',
    'providerType',
    'last4',
    'updatedAt',
    'actions',
  ];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<ProviderSecret[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = PROVIDER_SECRET_STATUSES;
  readonly types = PROVIDER_SECRET_TYPES;

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.statusControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.typeControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.load();
  }

  load(): void {
    if (!this.permissions.canReadProviderSecrets()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.secretsApi
      .listSecrets({
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        providerType: this.typeControl.value || undefined,
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
          this.error.set('Unable to load provider secrets.');
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
    void this.router.navigate(['/provider-secrets', 'new']);
  }

  open(secret: ProviderSecret): void {
    void this.router.navigate(['/provider-secrets', secret.id]);
  }

  statusClass(status: ProviderSecretStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
