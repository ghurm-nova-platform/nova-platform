import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { Project } from '../../core/models/catalog';
import { ProjectService } from '../projects/project.service';
import { OrchestrationPermissionHelper } from './orchestration-permission.helper';
import { OrchestrationRunService } from './orchestration-run.service';
import {
  EXECUTION_MODES,
  ExecutionMode,
  FAILURE_POLICIES,
  FailurePolicy,
  OrchestrationRun,
} from './orchestration.models';

@Component({
  selector: 'app-orchestration-run-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './orchestration-run-form-page.html',
  styleUrl: './orchestration-page.scss',
})
export class OrchestrationRunFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly runsApi = inject(OrchestrationRunService);
  private readonly projectsApi = inject(ProjectService);
  readonly permissions = inject(OrchestrationPermissionHelper);

  readonly runId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly currentRun = signal<OrchestrationRun | null>(null);
  readonly projects = signal<Project[]>([]);
  readonly modes = EXECUTION_MODES;
  readonly failurePolicies = FAILURE_POLICIES;

  readonly form = this.fb.nonNullable.group({
    projectId: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    objective: ['', [Validators.required, Validators.maxLength(4000)]],
    executionMode: ['DEPENDENCY_GRAPH' as ExecutionMode, Validators.required],
    failurePolicy: ['FAIL_FAST' as FailurePolicy, Validators.required],
    maxParallelTasks: [5, [Validators.required, Validators.min(1), Validators.max(100)]],
    maximumDurationMs: [3600000, [Validators.required, Validators.min(1000), Validators.max(86400000)]],
    initiatedByAgentId: [''],
    inputJson: ['', Validators.maxLength(100000)],
    metadataJson: ['', Validators.maxLength(100000)],
  });

  ngOnInit(): void {
    const runId = this.route.snapshot.paramMap.get('runId');
    this.runId.set(runId);
    this.editMode.set(!!runId && this.route.snapshot.url.some((segment) => segment.path === 'edit'));

    this.loadProjects();

    if (this.editMode() && runId) {
      if (!this.permissions.canUpdateRun()) {
        this.unauthorized.set(true);
        return;
      }
      this.form.controls.projectId.disable();
      this.loadRun(runId);
    } else if (!this.permissions.canCreateRun()) {
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
    const initiatedByAgentId = raw.initiatedByAgentId.trim() || null;
    const inputJson = raw.inputJson.trim() || null;
    const metadataJson = raw.metadataJson.trim() || null;

    if (this.editMode() && this.runId()) {
      const current = this.currentRun();
      if (!current) {
        this.saving.set(false);
        return;
      }
      this.runsApi
        .update(this.runId()!, {
          version: current.version,
          name: raw.name.trim(),
          objective: raw.objective.trim(),
          executionMode: raw.executionMode,
          failurePolicy: raw.failurePolicy,
          maxParallelTasks: raw.maxParallelTasks,
          maximumDurationMs: raw.maximumDurationMs,
          initiatedByAgentId,
          inputJson,
          metadataJson,
        })
        .subscribe({
          next: (run) => {
            this.saving.set(false);
            void this.router.navigate(['/orchestration-runs', run.id]);
          },
          error: (err: { status?: number; error?: { message?: string } }) => {
            this.saving.set(false);
            if (err.status === 403) {
              this.unauthorized.set(true);
              return;
            }
            this.error.set(err.error?.message ?? 'Unable to save run.');
          },
        });
      return;
    }

    this.runsApi
      .create({
        projectId: raw.projectId,
        name: raw.name.trim(),
        objective: raw.objective.trim(),
        executionMode: raw.executionMode,
        failurePolicy: raw.failurePolicy,
        maxParallelTasks: raw.maxParallelTasks,
        maximumDurationMs: raw.maximumDurationMs,
        initiatedByAgentId,
        inputJson,
        metadataJson,
      })
      .subscribe({
        next: (run) => {
          this.saving.set(false);
          void this.router.navigate(['/orchestration-runs', run.id, 'graph']);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create run.');
        },
      });
  }

  private loadProjects(): void {
    this.projectsApi.list({ page: 0, size: 100, sort: 'name,asc' }).subscribe({
      next: (page) => {
        this.projects.set(page.content);
        if (!this.editMode() && !this.form.controls.projectId.value && page.content.length > 0) {
          this.form.patchValue({ projectId: page.content[0].id });
        }
      },
      error: () => this.projects.set([]),
    });
  }

  private loadRun(runId: string): void {
    this.loading.set(true);
    this.runsApi.get(runId).subscribe({
      next: (run) => {
        if (run.status !== 'DRAFT') {
          this.error.set('Only DRAFT runs can be edited.');
          this.loading.set(false);
          return;
        }
        this.currentRun.set(run);
        this.form.patchValue({
          projectId: run.projectId,
          name: run.name,
          objective: run.objective,
          executionMode: run.executionMode,
          failurePolicy: run.failurePolicy,
          maxParallelTasks: run.maxParallelTasks,
          maximumDurationMs: run.maximumDurationMs,
          initiatedByAgentId: run.initiatedByAgentId ?? '',
          inputJson: run.inputJson ?? '',
          metadataJson: run.metadataJson ?? '',
        });
        this.loading.set(false);
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
}
