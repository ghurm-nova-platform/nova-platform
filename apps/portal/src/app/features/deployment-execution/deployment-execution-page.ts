import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { DeploymentExecutionPermissionHelper } from './deployment-execution-permission.helper';
import { DeploymentExecutionService } from './deployment-execution.service';
import { DeploymentExecution, ExecutionProviderCode } from './deployment-execution.models';

@Component({
  selector: 'app-deployment-execution-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './deployment-execution-page.html',
  styleUrl: './deployment-execution-page.scss',
})
export class DeploymentExecutionPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly executionApi = inject(DeploymentExecutionService);
  readonly permissions = inject(DeploymentExecutionPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly selected = signal<DeploymentExecution | null>(null);
  readonly executions = signal<DeploymentExecution[]>([]);
  readonly logs = signal<{ level: string; message: string; createdAt: string }[]>([]);

  readonly form = this.fb.nonNullable.group({
    projectId: [''],
    releaseId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    environmentId: ['bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    provider: ['LOCAL' as ExecutionProviderCode, Validators.required],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  create(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.runSingle(
      () =>
        this.executionApi.create({
          releaseId: value.releaseId,
          environmentId: value.environmentId,
          provider: value.provider,
        }),
      'Failed to create deployment execution',
      true,
    );
  }

  startSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.executionApi.start(id), 'Failed to start execution', true);
  }

  cancelSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.executionApi.cancel(id), 'Failed to cancel execution', true);
  }

  loadList(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    const projectId = this.form.controls.projectId.value || undefined;
    this.loading.set(true);
    this.error.set(null);
    this.executionApi.list(projectId).subscribe({
      next: (items) => {
        this.executions.set(items);
        if (items.length > 0 && !this.selected()) {
          this.selected.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load executions');
      },
    });
  }

  loadLogs(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRead() || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.executionApi.logs(id).subscribe({
      next: (entries) => {
        this.logs.set(entries);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load logs');
      },
    });
  }

  selectExecution(item: DeploymentExecution): void {
    this.selected.set(item);
    this.logs.set([]);
  }

  formatDuration(ms: number | null): string {
    if (ms == null) {
      return '—';
    }
    if (ms < 1000) {
      return `${ms} ms`;
    }
    return `${(ms / 1000).toFixed(1)} s`;
  }

  private runSingle(
    call: () => ReturnType<DeploymentExecutionService['create']>,
    fallback: string,
    refreshList: boolean,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    call().subscribe({
      next: (execution) => {
        this.selected.set(execution);
        this.loading.set(false);
        if (refreshList) {
          this.loadList();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? fallback);
      },
    });
  }
}
