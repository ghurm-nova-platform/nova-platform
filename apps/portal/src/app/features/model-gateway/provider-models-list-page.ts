import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  AI_MODEL_STATUSES,
  AI_MODEL_TYPES,
  AiModel,
  AiModelStatus,
  AiModelType,
} from './model-gateway.models';
import { AiModelService } from './ai-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';

@Component({
  selector: 'app-provider-models-list-page',
  imports: [
    DatePipe,
    RouterLink,
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
  templateUrl: './provider-models-list-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ProviderModelsListPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly modelsApi = inject(AiModelService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly providerId = signal('');
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<AiModelStatus | ''>('', { nonNullable: true });
  readonly typeControl = new FormControl<AiModelType | ''>('', { nonNullable: true });
  readonly displayedColumns = ['displayName', 'modelKey', 'status', 'modelType', 'updatedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<AiModel[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = AI_MODEL_STATUSES;
  readonly types = AI_MODEL_TYPES;

  ngOnInit(): void {
    this.providerId.set(this.route.snapshot.paramMap.get('providerId') ?? '');
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
    if (!this.permissions.canReadModels()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.modelsApi
      .listModels(this.providerId(), {
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        modelType: this.typeControl.value || undefined,
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
          this.error.set('Unable to load models.');
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
    void this.router.navigate(['/model-providers', this.providerId(), 'models', 'new']);
  }

  open(model: AiModel): void {
    void this.router.navigate(['/model-providers', this.providerId(), 'models', model.id]);
  }

  activate(model: AiModel): void {
    if (!this.permissions.canActivateModel() || !window.confirm(`Activate model "${model.displayName}"?`)) {
      return;
    }
    this.modelsApi.activateModel(this.providerId(), model.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to activate model.'),
    });
  }

  disable(model: AiModel): void {
    if (!this.permissions.canDisableModel() || !window.confirm(`Disable model "${model.displayName}"?`)) {
      return;
    }
    this.modelsApi.disableModel(this.providerId(), model.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to disable model.'),
    });
  }

  archive(model: AiModel): void {
    if (!this.permissions.canArchiveModel() || !window.confirm(`Archive model "${model.displayName}"?`)) {
      return;
    }
    this.modelsApi.archiveModel(this.providerId(), model.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to archive model.'),
    });
  }

  statusClass(status: AiModelStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
