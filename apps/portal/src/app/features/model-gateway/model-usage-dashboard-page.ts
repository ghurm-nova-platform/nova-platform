import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ModelUsageDaily, ModelUsageSummary } from './model-gateway.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelUsageService } from './model-usage.service';

@Component({
  selector: 'app-model-usage-dashboard-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './model-usage-dashboard-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ModelUsageDashboardPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly usageApi = inject(ModelUsageService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly projectId = signal('');
  readonly fromControl = new FormControl('', { nonNullable: true });
  readonly toControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = [
    'usageDate',
    'providerName',
    'displayName',
    'requestCount',
    'inputTokens',
    'outputTokens',
    'estimatedCost',
  ];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<ModelUsageDaily[]>([]);
  readonly summary = signal<ModelUsageSummary | null>(null);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.load();
  }

  load(): void {
    if (!this.permissions.canReadUsage()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.usageApi
      .getUsage(this.projectId(), {
        from: this.fromControl.value.trim() || undefined,
        to: this.toControl.value.trim() || undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: 'usageDate,desc',
      })
      .subscribe({
        next: (response) => {
          this.summary.set(response.summary);
          this.rows.set(response.daily);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: (err: { status?: number }) => {
          this.loading.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set('Unable to load model usage.');
        },
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  formatCost(value: number | null, currency: string | null): string {
    if (value === null) {
      return '—';
    }
    return currency ? `${value} ${currency}` : String(value);
  }
}
