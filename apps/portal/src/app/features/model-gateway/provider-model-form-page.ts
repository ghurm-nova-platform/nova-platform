import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AI_MODEL_TYPES, AiModel, AiModelType } from './model-gateway.models';
import { AiModelService } from './ai-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';

@Component({
  selector: 'app-provider-model-form-page',
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
  templateUrl: './provider-model-form-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ProviderModelFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly modelsApi = inject(AiModelService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly providerId = signal('');
  readonly modelId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly currentModel = signal<AiModel | null>(null);
  readonly types = AI_MODEL_TYPES;

  readonly form = this.fb.nonNullable.group({
    modelKey: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/), Validators.maxLength(100)]],
    providerModelId: ['', [Validators.required, Validators.maxLength(255)]],
    displayName: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    modelType: ['CHAT' as AiModelType, Validators.required],
    contextWindowTokens: [8192, [Validators.required, Validators.min(256), Validators.max(2000000)]],
    maxOutputTokens: [2048, [Validators.required, Validators.min(1)]],
    supportsTools: [false],
    supportsKnowledgeContext: [true],
    supportsJsonOutput: [false],
    supportsStreaming: [false],
    supportsSystemMessages: [true],
    inputCostPerMillion: [''],
    outputCostPerMillion: [''],
    currencyCode: ['', Validators.maxLength(3)],
  });

  ngOnInit(): void {
    this.providerId.set(this.route.snapshot.paramMap.get('providerId') ?? '');
    const modelId = this.route.snapshot.paramMap.get('modelId');
    this.modelId.set(modelId);
    this.editMode.set(!!modelId && modelId !== 'new');

    if (this.editMode() && modelId) {
      if (!this.permissions.canUpdateModel()) {
        this.unauthorized.set(true);
        return;
      }
      this.form.controls.modelKey.disable();
      this.form.controls.modelType.disable();
      this.loadModel(modelId);
    } else if (!this.permissions.canCreateModel()) {
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
    const raw = this.form.getRawValue();

    if (this.editMode() && this.modelId()) {
      const current = this.currentModel();
      if (!current) {
        return;
      }
      this.modelsApi
        .updateModel(this.providerId(), this.modelId()!, {
          version: current.version,
          displayName: raw.displayName.trim(),
          description: raw.description.trim() || null,
          providerModelId: raw.providerModelId.trim(),
          contextWindowTokens: raw.contextWindowTokens,
          maxOutputTokens: raw.maxOutputTokens,
          supportsTools: raw.supportsTools,
          supportsKnowledgeContext: raw.supportsKnowledgeContext,
          supportsJsonOutput: raw.supportsJsonOutput,
          supportsStreaming: raw.supportsStreaming,
          supportsSystemMessages: raw.supportsSystemMessages,
          inputCostPerMillion: this.parseOptionalNumber(raw.inputCostPerMillion),
          outputCostPerMillion: this.parseOptionalNumber(raw.outputCostPerMillion),
          currencyCode: raw.currencyCode.trim() || null,
        })
        .subscribe({
          next: (model) => {
            this.saving.set(false);
            this.currentModel.set(model);
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

    this.modelsApi
      .createModel(this.providerId(), {
        modelKey: raw.modelKey.trim(),
        providerModelId: raw.providerModelId.trim(),
        displayName: raw.displayName.trim(),
        description: raw.description.trim() || null,
        modelType: raw.modelType,
        contextWindowTokens: raw.contextWindowTokens,
        maxOutputTokens: raw.maxOutputTokens,
        supportsTools: raw.supportsTools,
        supportsKnowledgeContext: raw.supportsKnowledgeContext,
        supportsJsonOutput: raw.supportsJsonOutput,
        supportsStreaming: raw.supportsStreaming,
        supportsSystemMessages: raw.supportsSystemMessages,
        inputCostPerMillion: this.parseOptionalNumber(raw.inputCostPerMillion),
        outputCostPerMillion: this.parseOptionalNumber(raw.outputCostPerMillion),
        currencyCode: raw.currencyCode.trim() || null,
      })
      .subscribe({
        next: (model) => {
          this.saving.set(false);
          void this.router.navigate(['/model-providers', this.providerId(), 'models', model.id]);
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

  activate(): void {
    const current = this.currentModel();
    if (!current || !this.permissions.canActivateModel() || !window.confirm(`Activate "${current.displayName}"?`)) {
      return;
    }
    this.modelsApi.activateModel(this.providerId(), current.id).subscribe({
      next: (model) => this.currentModel.set(model),
      error: () => this.error.set('Unable to activate model.'),
    });
  }

  disable(): void {
    const current = this.currentModel();
    if (!current || !this.permissions.canDisableModel() || !window.confirm(`Disable "${current.displayName}"?`)) {
      return;
    }
    this.modelsApi.disableModel(this.providerId(), current.id).subscribe({
      next: (model) => this.currentModel.set(model),
      error: () => this.error.set('Unable to disable model.'),
    });
  }

  archive(): void {
    const current = this.currentModel();
    if (!current || !this.permissions.canArchiveModel() || !window.confirm(`Archive "${current.displayName}"?`)) {
      return;
    }
    this.modelsApi.archiveModel(this.providerId(), current.id).subscribe({
      next: () => void this.router.navigate(['/model-providers', this.providerId(), 'models']),
      error: () => this.error.set('Unable to archive model.'),
    });
  }

  statusClass(status: AiModel['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadModel(modelId: string): void {
    this.loading.set(true);
    this.modelsApi.getModel(this.providerId(), modelId).subscribe({
      next: (model) => {
        this.currentModel.set(model);
        this.form.patchValue({
          modelKey: model.modelKey,
          providerModelId: model.providerModelId,
          displayName: model.displayName,
          description: model.description ?? '',
          modelType: model.modelType,
          contextWindowTokens: model.contextWindowTokens,
          maxOutputTokens: model.maxOutputTokens,
          supportsTools: model.supportsTools,
          supportsKnowledgeContext: model.supportsKnowledgeContext,
          supportsJsonOutput: model.supportsJsonOutput,
          supportsStreaming: model.supportsStreaming,
          supportsSystemMessages: model.supportsSystemMessages,
          inputCostPerMillion: model.inputCostPerMillion?.toString() ?? '',
          outputCostPerMillion: model.outputCostPerMillion?.toString() ?? '',
          currencyCode: model.currencyCode ?? '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load model.');
        this.loading.set(false);
      },
    });
  }

  private parseOptionalNumber(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }
}
