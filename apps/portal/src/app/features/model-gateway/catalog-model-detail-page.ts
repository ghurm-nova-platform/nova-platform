import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CatalogModelService } from './catalog-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import {
  AI_MODEL_CAPABILITIES,
  AiModelCapability,
  CatalogModel,
  CatalogModelAlias,
} from './model-gateway.models';

@Component({
  selector: 'app-catalog-model-detail-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './catalog-model-detail-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class CatalogModelDetailPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogApi = inject(CatalogModelService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly modelId = signal('');
  readonly loading = signal(true);
  readonly savingCapabilities = signal(false);
  readonly savingAlias = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly model = signal<CatalogModel | null>(null);
  readonly aliases = signal<CatalogModelAlias[]>([]);
  readonly selectedCapabilities = signal<Set<AiModelCapability>>(new Set());
  readonly allCapabilities = AI_MODEL_CAPABILITIES;

  readonly aliasForm = this.fb.nonNullable.group({
    alias: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.modelId.set(this.route.snapshot.paramMap.get('modelId') ?? '');
    this.load();
  }

  edit(): void {
    void this.router.navigate(['/ai-models', this.modelId(), 'edit']);
  }

  activate(): void {
    const current = this.model();
    if (!current || !this.permissions.canUpdateCatalog() || !window.confirm(`Activate "${current.displayName}"?`)) {
      return;
    }
    this.catalogApi.activate(current.id).subscribe({
      next: (model) => this.model.set(model),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Unable to activate model.'),
    });
  }

  disable(): void {
    const current = this.model();
    if (!current || !this.permissions.canUpdateCatalog() || !window.confirm(`Disable "${current.displayName}"?`)) {
      return;
    }
    this.catalogApi.disable(current.id).subscribe({
      next: (model) => this.model.set(model),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Unable to disable model.'),
    });
  }

  deprecate(): void {
    const current = this.model();
    if (!current || !this.permissions.canUpdateCatalog() || !window.confirm(`Deprecate "${current.displayName}"?`)) {
      return;
    }
    this.catalogApi.deprecate(current.id).subscribe({
      next: (model) => this.model.set(model),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Unable to deprecate model.'),
    });
  }

  archive(): void {
    const current = this.model();
    if (!current || !this.permissions.canDeleteCatalog() || !window.confirm(`Archive "${current.displayName}"?`)) {
      return;
    }
    this.catalogApi.archive(current.id).subscribe({
      next: () => void this.router.navigate(['/ai-models']),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Unable to archive model.'),
    });
  }

  isCapabilitySelected(capability: AiModelCapability): boolean {
    return this.selectedCapabilities().has(capability);
  }

  toggleCapability(capability: AiModelCapability, checked: boolean): void {
    this.selectedCapabilities.update((current) => {
      const next = new Set(current);
      if (checked) {
        next.add(capability);
      } else {
        next.delete(capability);
      }
      return next;
    });
  }

  saveCapabilities(): void {
    const current = this.model();
    if (!current || !this.permissions.canManageCapabilities() || this.savingCapabilities()) {
      return;
    }
    this.savingCapabilities.set(true);
    this.error.set(null);
    const capabilities = [...this.selectedCapabilities()].map((capability) => ({
      capability,
      enabled: true,
    }));
    this.catalogApi.replaceCapabilities(current.id, capabilities).subscribe({
      next: (model) => {
        this.savingCapabilities.set(false);
        this.applyModel(model);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.savingCapabilities.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to update capabilities.');
      },
    });
  }

  createAlias(): void {
    const current = this.model();
    if (
      !current ||
      !this.permissions.canManageAliases() ||
      this.aliasForm.invalid ||
      this.savingAlias()
    ) {
      this.aliasForm.markAllAsTouched();
      return;
    }
    this.savingAlias.set(true);
    this.error.set(null);
    this.catalogApi.createAlias(current.id, { alias: this.aliasForm.controls.alias.value.trim() }).subscribe({
      next: (alias) => {
        this.savingAlias.set(false);
        this.aliasForm.reset({ alias: '' });
        this.aliases.update((rows) => [...rows, alias]);
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.savingAlias.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to create alias.');
      },
    });
  }

  deleteAlias(alias: CatalogModelAlias): void {
    if (!this.permissions.canManageAliases() || !window.confirm(`Delete alias "${alias.alias}"?`)) {
      return;
    }
    this.catalogApi.deleteAlias(alias.id).subscribe({
      next: () => this.aliases.update((rows) => rows.filter((row) => row.id !== alias.id)),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Unable to delete alias.'),
    });
  }

  statusClass(status: CatalogModel['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private load(): void {
    if (!this.permissions.canReadCatalog()) {
      this.unauthorized.set(true);
      this.loading.set(false);
      return;
    }
    this.catalogApi.get(this.modelId()).subscribe({
      next: (model) => {
        this.applyModel(model);
        this.loading.set(false);
        this.loadAliases();
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load model.');
      },
    });
  }

  private loadAliases(): void {
    this.catalogApi.listAliases(this.modelId()).subscribe({
      next: (aliases) => this.aliases.set(aliases),
      error: () => this.aliases.set([]),
    });
  }

  private applyModel(model: CatalogModel): void {
    this.model.set(model);
    this.selectedCapabilities.set(
      new Set(
        (model.capabilities ?? [])
          .filter((cap) => cap.enabled)
          .map((cap) => cap.capability),
      ),
    );
  }
}
