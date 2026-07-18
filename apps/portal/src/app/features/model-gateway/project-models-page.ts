import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AiModelStatus, ProjectModelAssignment } from './model-gateway.models';
import { AiModelService } from './ai-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelProviderService } from './model-provider.service';

interface AssignableModelOption {
  id: string;
  label: string;
}

@Component({
  selector: 'app-project-models-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './project-models-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ProjectModelsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly modelsApi = inject(AiModelService);
  private readonly providersApi = inject(ModelProviderService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly projectId = signal('');
  readonly loading = signal(false);
  readonly assigning = signal(false);
  readonly updating = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly assignments = signal<ProjectModelAssignment[]>([]);
  readonly availableModels = signal<AssignableModelOption[]>([]);
  readonly displayedColumns = [
    'displayName',
    'modelKey',
    'providerName',
    'modelStatus',
    'enabled',
    'isDefault',
    'updatedAt',
    'actions',
  ];
  readonly assignForm = this.fb.nonNullable.group({
    modelId: ['', Validators.required],
    enabled: [true],
    isDefault: [false],
    maximumInputTokensOverride: [''],
    maximumOutputTokensOverride: [''],
    dailyRequestLimit: [''],
    monthlyRequestLimit: [''],
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.load();
  }

  load(): void {
    if (!this.permissions.canReadModels()) {
      this.unauthorized.set(true);
      this.assignments.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.modelsApi.listProjectModels(this.projectId()).subscribe({
      next: (rows) => {
        this.assignments.set(rows);
        this.loading.set(false);
        this.loadAvailableModels(rows);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load project models.');
      },
    });
  }

  assign(): void {
    if (!this.permissions.canAssignProjectModels() || this.assignForm.invalid || this.assigning()) {
      this.assignForm.markAllAsTouched();
      return;
    }
    this.assigning.set(true);
    this.error.set(null);
    const raw = this.assignForm.getRawValue();
    this.modelsApi
      .assignProjectModel(this.projectId(), {
        modelId: raw.modelId,
        enabled: raw.enabled,
        isDefault: raw.isDefault,
        maximumInputTokensOverride: this.parseOptionalNumber(raw.maximumInputTokensOverride),
        maximumOutputTokensOverride: this.parseOptionalNumber(raw.maximumOutputTokensOverride),
        dailyRequestLimit: this.parseOptionalNumber(raw.dailyRequestLimit),
        monthlyRequestLimit: this.parseOptionalNumber(raw.monthlyRequestLimit),
      })
      .subscribe({
        next: () => {
          this.assignForm.reset({ enabled: true, isDefault: false });
          this.assigning.set(false);
          this.load();
        },
        error: (err: { error?: { message?: string } }) => {
          this.assigning.set(false);
          this.error.set(err.error?.message ?? 'Unable to assign model.');
        },
      });
  }

  toggleEnabled(assignment: ProjectModelAssignment): void {
    if (!this.permissions.canAssignProjectModels() || this.updating()) {
      return;
    }
    this.updating.set(true);
    this.modelsApi
      .updateProjectModel(this.projectId(), assignment.modelId, {
        version: assignment.version,
        enabled: !assignment.enabled,
        isDefault: assignment.isDefault,
        maximumInputTokensOverride: assignment.maximumInputTokensOverride,
        maximumOutputTokensOverride: assignment.maximumOutputTokensOverride,
        dailyRequestLimit: assignment.dailyRequestLimit,
        monthlyRequestLimit: assignment.monthlyRequestLimit,
      })
      .subscribe({
        next: () => {
          this.updating.set(false);
          this.load();
        },
        error: () => {
          this.updating.set(false);
          this.error.set('Unable to update assignment.');
        },
      });
  }

  setDefault(assignment: ProjectModelAssignment): void {
    if (!this.permissions.canAssignProjectModels() || this.updating() || assignment.isDefault) {
      return;
    }
    this.updating.set(true);
    this.modelsApi
      .updateProjectModel(this.projectId(), assignment.modelId, {
        version: assignment.version,
        enabled: assignment.enabled,
        isDefault: true,
        maximumInputTokensOverride: assignment.maximumInputTokensOverride,
        maximumOutputTokensOverride: assignment.maximumOutputTokensOverride,
        dailyRequestLimit: assignment.dailyRequestLimit,
        monthlyRequestLimit: assignment.monthlyRequestLimit,
      })
      .subscribe({
        next: () => {
          this.updating.set(false);
          this.load();
        },
        error: () => {
          this.updating.set(false);
          this.error.set('Unable to set default model.');
        },
      });
  }

  unassign(assignment: ProjectModelAssignment): void {
    if (
      !this.permissions.canAssignProjectModels() ||
      !window.confirm(`Unassign model "${assignment.displayName}" from this project?`)
    ) {
      return;
    }
    this.modelsApi.unassignProjectModel(this.projectId(), assignment.modelId).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to unassign model.'),
    });
  }

  statusClass(status: AiModelStatus): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadAvailableModels(assignments: ProjectModelAssignment[]): void {
    if (!this.permissions.canAssignProjectModels()) {
      this.availableModels.set([]);
      return;
    }
    this.providersApi.listProviders({ status: 'ACTIVE', size: 100 }).subscribe({
      next: (providerPage) => {
        const assignedIds = new Set(assignments.map((row) => row.modelId));
        const options: AssignableModelOption[] = [];
        let pending = providerPage.content.length;
        if (pending === 0) {
          this.availableModels.set([]);
          return;
        }
        for (const provider of providerPage.content) {
          this.modelsApi.listModels(provider.id, { status: 'ACTIVE', size: 100 }).subscribe({
            next: (modelPage) => {
              for (const model of modelPage.content) {
                if (!assignedIds.has(model.id)) {
                  options.push({
                    id: model.id,
                    label: `${model.displayName} (${provider.name})`,
                  });
                }
              }
              pending -= 1;
              if (pending === 0) {
                this.availableModels.set(options.sort((a, b) => a.label.localeCompare(b.label)));
              }
            },
            error: () => {
              pending -= 1;
              if (pending === 0) {
                this.availableModels.set(options);
              }
            },
          });
        }
      },
      error: () => this.error.set('Unable to load available models.'),
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
