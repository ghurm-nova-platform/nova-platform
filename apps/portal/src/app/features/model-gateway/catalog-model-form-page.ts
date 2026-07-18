import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { CatalogModelService } from './catalog-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelProviderService } from './model-provider.service';
import {
  AI_MODEL_CAPABILITIES,
  AI_MODEL_TYPES,
  AiModelCapability,
  AiModelType,
  CatalogModel,
  ModelProvider,
} from './model-gateway.models';

@Component({
  selector: 'app-catalog-model-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './catalog-model-form-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class CatalogModelFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogApi = inject(CatalogModelService);
  private readonly providersApi = inject(ModelProviderService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly modelId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly currentModel = signal<CatalogModel | null>(null);
  readonly providers = signal<ModelProvider[]>([]);
  readonly types = AI_MODEL_TYPES;
  readonly allCapabilities = AI_MODEL_CAPABILITIES;
  readonly selectedCapabilities = signal<Set<AiModelCapability>>(new Set(['CHAT']));

  readonly form = this.fb.nonNullable.group({
    providerId: ['', Validators.required],
    modelKey: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/), Validators.maxLength(100)]],
    providerModelId: ['', [Validators.required, Validators.maxLength(255)]],
    displayName: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    modelType: ['CHAT' as AiModelType, Validators.required],
    modelFamily: ['', Validators.maxLength(100)],
    modelVersion: ['', Validators.maxLength(100)],
    contextWindowTokens: [8192, [Validators.required, Validators.min(256), Validators.max(2000000)]],
    maxInputTokens: [''],
    maxOutputTokens: [2048, [Validators.required, Validators.min(1)]],
    defaultTemperature: [''],
    defaultTopP: [''],
    defaultMaxOutputTokens: [''],
    supportsKnowledgeContext: [true],
    supportsSystemMessages: [true],
    inputCostPerMillion: [''],
    outputCostPerMillion: [''],
    currency: ['', Validators.maxLength(3)],
  });

  ngOnInit(): void {
    const modelId = this.route.snapshot.paramMap.get('modelId');
    this.modelId.set(modelId);
    this.editMode.set(!!modelId && this.route.snapshot.url.some((segment) => segment.path === 'edit'));

    this.loadProviders();

    if (this.editMode() && modelId) {
      if (!this.permissions.canUpdateCatalog()) {
        this.unauthorized.set(true);
        return;
      }
      this.form.controls.providerId.disable();
      this.form.controls.modelKey.disable();
      this.form.controls.providerModelId.disable();
      this.form.controls.modelType.disable();
      this.loadModel(modelId);
    } else if (!this.permissions.canCreateCatalog()) {
      this.unauthorized.set(true);
    }
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

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();

    if (this.editMode() && this.modelId()) {
      const current = this.currentModel();
      if (!current) {
        this.saving.set(false);
        return;
      }
      this.catalogApi
        .update(this.modelId()!, {
          version: current.version,
          displayName: raw.displayName.trim(),
          description: raw.description.trim() || null,
          modelFamily: raw.modelFamily.trim() || null,
          modelVersion: raw.modelVersion.trim() || null,
          contextWindowTokens: raw.contextWindowTokens,
          maxInputTokens: this.parseOptionalNumber(raw.maxInputTokens),
          maxOutputTokens: raw.maxOutputTokens,
          defaultTemperature: this.parseOptionalNumber(raw.defaultTemperature),
          defaultTopP: this.parseOptionalNumber(raw.defaultTopP),
          defaultMaxOutputTokens: this.parseOptionalNumber(raw.defaultMaxOutputTokens),
          supportsKnowledgeContext: raw.supportsKnowledgeContext,
          supportsSystemMessages: raw.supportsSystemMessages,
          inputCostPerMillion: this.parseOptionalNumber(raw.inputCostPerMillion),
          outputCostPerMillion: this.parseOptionalNumber(raw.outputCostPerMillion),
          currency: raw.currency.trim() || null,
        })
        .subscribe({
          next: (model) => {
            this.saving.set(false);
            void this.router.navigate(['/ai-models', model.id]);
          },
          error: (err: { status?: number; error?: { message?: string } }) => {
            this.saving.set(false);
            if (err.status === 403) {
              this.unauthorized.set(true);
              return;
            }
            this.error.set(err.error?.message ?? 'Unable to save model.');
          },
        });
      return;
    }

    this.catalogApi
      .create({
        providerId: raw.providerId,
        modelKey: raw.modelKey.trim(),
        providerModelId: raw.providerModelId.trim(),
        displayName: raw.displayName.trim(),
        description: raw.description.trim() || null,
        modelType: raw.modelType,
        modelFamily: raw.modelFamily.trim() || null,
        modelVersion: raw.modelVersion.trim() || null,
        contextWindowTokens: raw.contextWindowTokens,
        maxInputTokens: this.parseOptionalNumber(raw.maxInputTokens),
        maxOutputTokens: raw.maxOutputTokens,
        defaultTemperature: this.parseOptionalNumber(raw.defaultTemperature),
        defaultTopP: this.parseOptionalNumber(raw.defaultTopP),
        defaultMaxOutputTokens: this.parseOptionalNumber(raw.defaultMaxOutputTokens),
        supportsKnowledgeContext: raw.supportsKnowledgeContext,
        supportsSystemMessages: raw.supportsSystemMessages,
        inputCostPerMillion: this.parseOptionalNumber(raw.inputCostPerMillion),
        outputCostPerMillion: this.parseOptionalNumber(raw.outputCostPerMillion),
        currency: raw.currency.trim() || null,
        capabilities: [...this.selectedCapabilities()],
      })
      .subscribe({
        next: (model) => {
          this.saving.set(false);
          void this.router.navigate(['/ai-models', model.id]);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create model.');
        },
      });
  }

  private loadProviders(): void {
    this.providersApi.listProviders({ page: 0, size: 100, sort: 'name,asc' }).subscribe({
      next: (page) => this.providers.set(page.content),
      error: () => this.providers.set([]),
    });
  }

  private loadModel(modelId: string): void {
    this.loading.set(true);
    this.catalogApi.get(modelId).subscribe({
      next: (model) => {
        this.currentModel.set(model);
        this.form.patchValue({
          providerId: model.providerId,
          modelKey: model.modelKey,
          providerModelId: model.providerModelId,
          displayName: model.displayName,
          description: model.description ?? '',
          modelType: model.modelType,
          modelFamily: model.modelFamily ?? '',
          modelVersion: model.modelVersion ?? '',
          contextWindowTokens: model.contextWindowTokens,
          maxInputTokens: model.maxInputTokens?.toString() ?? '',
          maxOutputTokens: model.maxOutputTokens,
          defaultTemperature: model.defaultTemperature?.toString() ?? '',
          defaultTopP: model.defaultTopP?.toString() ?? '',
          defaultMaxOutputTokens: model.defaultMaxOutputTokens?.toString() ?? '',
          supportsKnowledgeContext: model.supportsKnowledgeContext,
          supportsSystemMessages: model.supportsSystemMessages,
          inputCostPerMillion: model.inputCostPerMillion?.toString() ?? '',
          outputCostPerMillion: model.outputCostPerMillion?.toString() ?? '',
          currency: model.currency ?? '',
        });
        this.selectedCapabilities.set(
          new Set(
            (model.capabilities ?? [])
              .filter((cap) => cap.enabled)
              .map((cap) => cap.capability),
          ),
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load model.');
        this.loading.set(false);
      },
    });
  }

  private parseOptionalNumber(value: string | number): number | null {
    if (typeof value === 'number') {
      return Number.isFinite(value) ? value : null;
    }
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }
}
