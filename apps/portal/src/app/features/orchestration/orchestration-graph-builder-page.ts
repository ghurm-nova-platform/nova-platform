import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { OrchestrationPermissionHelper } from './orchestration-permission.helper';
import { OrchestrationRunService } from './orchestration-run.service';
import {
  DEPENDENCY_TYPES,
  DependencyType,
  OrchestrationDependency,
  OrchestrationRun,
  OrchestrationTask,
  TASK_TYPES,
  TaskType,
} from './orchestration.models';

@Component({
  selector: 'app-orchestration-graph-builder-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './orchestration-graph-builder-page.html',
  styleUrl: './orchestration-page.scss',
})
export class OrchestrationGraphBuilderPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly runsApi = inject(OrchestrationRunService);
  readonly permissions = inject(OrchestrationPermissionHelper);

  readonly runId = signal('');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly acting = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly run = signal<OrchestrationRun | null>(null);
  readonly tasks = signal<OrchestrationTask[]>([]);
  readonly edges = signal<OrchestrationDependency[]>([]);
  readonly editingTaskId = signal<string | null>(null);
  readonly taskTypes = TASK_TYPES;
  readonly dependencyTypes = DEPENDENCY_TYPES;
  readonly taskColumns = ['taskKey', 'displayName', 'taskType', 'priority', 'sequenceOrder', 'actions'];

  readonly taskForm = this.fb.nonNullable.group({
    taskKey: ['', [Validators.required, Validators.maxLength(150)]],
    displayName: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    taskType: ['AGENT_TURN' as TaskType, Validators.required],
    assignedAgentId: [''],
    modelReference: ['', Validators.maxLength(150)],
    inputJson: ['', Validators.maxLength(100000)],
    maxAttempts: [3, [Validators.min(1), Validators.max(20)]],
    retryBackoffMs: [1000, [Validators.min(0), Validators.max(3600000)]],
    priority: [100, [Validators.min(1), Validators.max(1000)]],
    timeoutSeconds: [300, [Validators.min(1), Validators.max(3600)]],
    sequenceOrder: [0],
    idempotencyKey: ['', Validators.maxLength(150)],
  });

  readonly dependencyForm = this.fb.nonNullable.group({
    predecessorTaskId: ['', Validators.required],
    successorTaskId: ['', Validators.required],
    dependencyType: ['SUCCESS' as DependencyType, Validators.required],
  });

  ngOnInit(): void {
    this.runId.set(this.route.snapshot.paramMap.get('runId') ?? '');
    if (!this.permissions.canReadRuns()) {
      this.unauthorized.set(true);
      this.loading.set(false);
      return;
    }
    this.load();
  }

  resetTaskForm(): void {
    this.editingTaskId.set(null);
    this.taskForm.reset({
      taskKey: '',
      displayName: '',
      description: '',
      taskType: 'AGENT_TURN',
      assignedAgentId: '',
      modelReference: '',
      inputJson: '',
      maxAttempts: 3,
      retryBackoffMs: 1000,
      priority: 100,
      timeoutSeconds: 300,
      sequenceOrder: this.tasks().length,
      idempotencyKey: '',
    });
  }

  editTask(task: OrchestrationTask): void {
    this.editingTaskId.set(task.id);
    this.taskForm.patchValue({
      taskKey: task.taskKey,
      displayName: task.displayName,
      description: task.description ?? '',
      taskType: task.taskType,
      assignedAgentId: task.assignedAgentId ?? '',
      modelReference: task.modelReference ?? '',
      inputJson: task.inputJson ?? '',
      maxAttempts: task.maxAttempts,
      retryBackoffMs: task.retryBackoffMs,
      priority: task.priority,
      timeoutSeconds: task.timeoutSeconds,
      sequenceOrder: task.sequenceOrder ?? 0,
      idempotencyKey: task.idempotencyKey ?? '',
    });
  }

  saveTask(): void {
    if (!this.permissions.canManageTasks() || this.taskForm.invalid || this.saving()) {
      this.taskForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const raw = this.taskForm.getRawValue();
    const body = {
      taskKey: raw.taskKey.trim(),
      displayName: raw.displayName.trim(),
      description: raw.description.trim() || null,
      taskType: raw.taskType,
      assignedAgentId: raw.assignedAgentId.trim() || null,
      modelReference: raw.modelReference.trim() || null,
      inputJson: raw.inputJson.trim() || null,
      maxAttempts: raw.maxAttempts,
      retryBackoffMs: raw.retryBackoffMs,
      priority: raw.priority,
      timeoutSeconds: raw.timeoutSeconds,
      sequenceOrder: raw.sequenceOrder,
      idempotencyKey: raw.idempotencyKey.trim() || null,
    };

    const editingId = this.editingTaskId();
    if (editingId) {
      const existing = this.tasks().find((task) => task.id === editingId);
      if (!existing) {
        this.saving.set(false);
        return;
      }
      this.runsApi.updateTask(this.runId(), editingId, { version: existing.version, ...body }).subscribe({
        next: () => {
          this.saving.set(false);
          this.resetTaskForm();
          this.loadGraph();
        },
        error: (err: { error?: { message?: string } }) => {
          this.saving.set(false);
          this.error.set(err.error?.message ?? 'Unable to update task.');
        },
      });
      return;
    }

    this.runsApi.createTask(this.runId(), body).subscribe({
      next: () => {
        this.saving.set(false);
        this.resetTaskForm();
        this.loadGraph();
      },
      error: (err: { error?: { message?: string } }) => {
        this.saving.set(false);
        this.error.set(err.error?.message ?? 'Unable to create task.');
      },
    });
  }

  deleteTask(task: OrchestrationTask): void {
    if (!this.permissions.canManageTasks() || !window.confirm(`Delete task "${task.displayName}"?`)) {
      return;
    }
    this.runsApi.deleteTask(this.runId(), task.id).subscribe({
      next: () => {
        if (this.editingTaskId() === task.id) {
          this.resetTaskForm();
        }
        this.loadGraph();
      },
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Unable to delete task.'),
    });
  }

  addDependency(): void {
    if (!this.permissions.canManageTasks() || this.dependencyForm.invalid || this.saving()) {
      this.dependencyForm.markAllAsTouched();
      return;
    }
    const raw = this.dependencyForm.getRawValue();
    if (raw.predecessorTaskId === raw.successorTaskId) {
      this.error.set('Predecessor and successor must be different tasks.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.runsApi
      .addDependency(this.runId(), {
        predecessorTaskId: raw.predecessorTaskId,
        successorTaskId: raw.successorTaskId,
        dependencyType: raw.dependencyType,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.dependencyForm.patchValue({ predecessorTaskId: '', successorTaskId: '' });
          this.loadGraph();
        },
        error: (err: { error?: { message?: string } }) => {
          this.saving.set(false);
          this.error.set(err.error?.message ?? 'Unable to add dependency.');
        },
      });
  }

  removeDependency(edge: OrchestrationDependency): void {
    if (!this.permissions.canManageTasks()) {
      return;
    }
    this.runsApi
      .removeDependency(this.runId(), {
        predecessorTaskId: edge.predecessorTaskId,
        successorTaskId: edge.successorTaskId,
      })
      .subscribe({
        next: () => this.loadGraph(),
        error: (err: { error?: { message?: string } }) =>
          this.error.set(err.error?.message ?? 'Unable to remove dependency.'),
      });
  }

  taskLabel(taskId: string): string {
    const task = this.tasks().find((row) => row.id === taskId);
    return task ? `${task.taskKey} (${task.displayName})` : taskId;
  }

  statusClass(status: string): string {
    return `status status--${status.toLowerCase().replace(/_/g, '-')}`;
  }

  markReady(): void {
    if (!this.permissions.canStartRun() || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.runsApi.ready(this.runId()).subscribe({
      next: (run) => {
        this.acting.set(false);
        this.run.set(run);
        void this.router.navigate(['/orchestration-runs', run.id]);
      },
      error: (err: { error?: { message?: string } }) => {
        this.acting.set(false);
        this.error.set(err.error?.message ?? 'Unable to mark run ready.');
      },
    });
  }

  start(): void {
    if (!this.permissions.canStartRun() || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.runsApi.start(this.runId()).subscribe({
      next: (run) => {
        this.acting.set(false);
        void this.router.navigate(['/orchestration-runs', run.id]);
      },
      error: (err: { error?: { message?: string } }) => {
        this.acting.set(false);
        this.error.set(err.error?.message ?? 'Unable to start run.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.runsApi.get(this.runId()).subscribe({
      next: (run) => {
        this.run.set(run);
        this.loading.set(false);
        this.loadGraph();
        this.resetTaskForm();
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load run.');
      },
    });
  }

  private loadGraph(): void {
    this.runsApi.getGraph(this.runId()).subscribe({
      next: (graph) => {
        this.tasks.set(graph.nodes);
        this.edges.set(graph.edges);
      },
      error: () => {
        this.tasks.set([]);
        this.edges.set([]);
      },
    });
  }
}
