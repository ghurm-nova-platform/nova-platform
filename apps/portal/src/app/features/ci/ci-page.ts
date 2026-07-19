import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CiPermissionHelper } from './ci-permission.helper';
import { CiService } from './ci.service';
import { CiJob, CiObservationOperation, CiStep, CiWorkflowRun } from './ci.models';

@Component({
  selector: 'app-ci-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './ci-page.html',
  styleUrl: './ci-page.scss',
})
export class CiPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ciApi = inject(CiService);
  readonly permissions = inject(CiPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<CiObservationOperation | null>(null);
  readonly history = signal<CiObservationOperation[]>([]);

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  runObservation(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.ciApi.run(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to run CI observation agent');
      },
    });
  }

  loadLatest(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.ciApi.getLatest(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load CI observation');
      },
    });
  }

  loadHistory(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.ciApi.getHistory(this.form.controls.taskId.value.trim()).subscribe({
      next: (items) => {
        this.history.set(items);
        if (items.length > 0) {
          this.result.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load CI observation history');
      },
    });
  }

  repositoryLabel(op: CiObservationOperation): string {
    return `${op.repositoryOwner}/${op.repositoryName}`;
  }

  formatDuration(ms: number | null | undefined): string {
    if (ms === null || ms === undefined) {
      return '—';
    }
    if (ms < 1000) {
      return `${ms} ms`;
    }
    const seconds = Math.floor(ms / 1000);
    if (seconds < 60) {
      return `${seconds}s`;
    }
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}m ${remainingSeconds}s`;
  }

  isFailedJob(job: CiJob): boolean {
    const conclusion = (job.conclusion ?? job.status ?? '').toLowerCase();
    return conclusion === 'failure' || conclusion === 'failed' || conclusion === 'cancelled';
  }

  isFailedStep(step: CiStep): boolean {
    const conclusion = (step.conclusion ?? step.status ?? '').toLowerCase();
    return conclusion === 'failure' || conclusion === 'failed' || conclusion === 'cancelled';
  }

  failedJobs(workflow: CiWorkflowRun): CiJob[] {
    return workflow.jobs.filter((job) => this.isFailedJob(job));
  }

  failedSteps(job: CiJob): CiStep[] {
    return job.steps.filter((step) => this.isFailedStep(step));
  }
}
