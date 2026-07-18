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

import {
  ASSIGNMENT_ROLES,
  AgentModelAssignment,
  AiModelStatus,
  AssignmentRole,
  ProjectModelAssignment,
} from './model-gateway.models';
import { AiModelService } from './ai-model.service';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';

@Component({
  selector: 'app-agent-models-page',
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
  templateUrl: './agent-models-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class AgentModelsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly modelsApi = inject(AiModelService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly projectId = signal('');
  readonly agentId = signal('');
  readonly loading = signal(false);
  readonly assigning = signal(false);
  readonly updating = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly assignments = signal<AgentModelAssignment[]>([]);
  readonly availableModels = signal<ProjectModelAssignment[]>([]);
  readonly roles = ASSIGNMENT_ROLES;
  readonly displayedColumns = [
    'assignmentRole',
    'priority',
    'displayName',
    'modelKey',
    'providerName',
    'modelStatus',
    'enabled',
    'updatedAt',
    'actions',
  ];
  readonly assignForm = this.fb.nonNullable.group({
    modelId: ['', Validators.required],
    assignmentRole: ['PRIMARY' as AssignmentRole, Validators.required],
    priority: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
    enabled: [true],
    temperatureOverride: [''],
    maximumOutputTokensOverride: [''],
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.agentId.set(this.route.snapshot.paramMap.get('agentId') ?? '');
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
    this.modelsApi.listAgentModels(this.projectId(), this.agentId()).subscribe({
      next: (rows) => {
        this.assignments.set(rows.sort((a, b) => a.priority - b.priority));
        this.loading.set(false);
        this.loadAvailableModels(rows);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load agent model assignments.');
      },
    });
  }

  assign(): void {
    if (!this.permissions.canAssignAgentModels() || this.assignForm.invalid || this.assigning()) {
      this.assignForm.markAllAsTouched();
      return;
    }
    this.assigning.set(true);
    this.error.set(null);
    const raw = this.assignForm.getRawValue();
    this.modelsApi
      .assignAgentModel(this.projectId(), this.agentId(), {
        modelId: raw.modelId,
        assignmentRole: raw.assignmentRole,
        priority: raw.priority,
        enabled: raw.enabled,
        temperatureOverride: this.parseOptionalNumber(raw.temperatureOverride),
        maximumOutputTokensOverride: this.parseOptionalNumber(raw.maximumOutputTokensOverride),
      })
      .subscribe({
        next: () => {
          this.assignForm.reset({ assignmentRole: 'PRIMARY', priority: 1, enabled: true });
          this.assigning.set(false);
          this.load();
        },
        error: (err: { error?: { message?: string } }) => {
          this.assigning.set(false);
          this.error.set(err.error?.message ?? 'Unable to assign model.');
        },
      });
  }

  updatePriority(assignment: AgentModelAssignment): void {
    if (!this.permissions.canAssignAgentModels() || this.updating()) {
      return;
    }
    const priorityRaw = window.prompt('Priority (1-10)', assignment.priority.toString());
    if (priorityRaw === null) {
      return;
    }
    const priority = Number(priorityRaw);
    if (!Number.isFinite(priority) || priority < 1 || priority > 10) {
      this.error.set('Priority must be between 1 and 10.');
      return;
    }
    this.updating.set(true);
    this.modelsApi
      .updateAgentModel(this.projectId(), this.agentId(), assignment.modelId, {
        version: assignment.version,
        priority,
        enabled: assignment.enabled,
        temperatureOverride: assignment.temperatureOverride,
        maximumOutputTokensOverride: assignment.maximumOutputTokensOverride,
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

  unassign(assignment: AgentModelAssignment): void {
    if (
      !this.permissions.canAssignAgentModels() ||
      !window.confirm(`Unassign ${assignment.assignmentRole} model "${assignment.displayName}"?`)
    ) {
      return;
    }
    this.modelsApi.unassignAgentModel(this.projectId(), this.agentId(), assignment.modelId).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to unassign model.'),
    });
  }

  statusClass(status: AiModelStatus): string {
    return `status status--${status.toLowerCase()}`;
  }

  roleClass(role: AssignmentRole): string {
    return `status status--${role.toLowerCase()}`;
  }

  private loadAvailableModels(assignments: AgentModelAssignment[]): void {
    if (!this.permissions.canAssignAgentModels()) {
      this.availableModels.set([]);
      return;
    }
    this.modelsApi.listProjectModels(this.projectId()).subscribe({
      next: (rows) => {
        const assignedIds = new Set(assignments.map((row) => row.modelId));
        this.availableModels.set(rows.filter((row) => row.enabled && !assignedIds.has(row.modelId)));
      },
      error: () => this.error.set('Unable to load project models.'),
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
