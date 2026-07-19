import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { DeploymentPermissionHelper } from './deployment-permission.helper';
import { DeploymentService } from './deployment.service';
import { Deployment, DeploymentEnvironment, DeploymentHealth, DeploymentStatus } from './deployment.models';

@Component({
  selector: 'app-deployment-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './deployment-page.html',
  styleUrl: './deployment-page.scss',
})
export class DeploymentPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly deploymentApi = inject(DeploymentService);
  readonly permissions = inject(DeploymentPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly selected = signal<Deployment | null>(null);
  readonly deployments = signal<Deployment[]>([]);
  readonly environments = signal<DeploymentEnvironment[]>([]);

  readonly form = this.fb.nonNullable.group({
    projectId: [''],
    releaseId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    environment: ['STAGING', Validators.required],
    deploymentProvider: ['LOCAL', Validators.required],
    externalDeploymentKey: [''],
    status: ['RUNNING' as DeploymentStatus, Validators.required],
    health: ['HEALTHY' as DeploymentHealth, Validators.required],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
      return;
    }
    if (this.permissions.canRead()) {
      this.loadEnvironments();
    }
  }

  observe(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.runSingle(
      () =>
        this.deploymentApi.observe({
          releaseId: value.releaseId,
          environment: value.environment,
          deploymentProvider: value.deploymentProvider,
          externalDeploymentKey: value.externalDeploymentKey || undefined,
          status: value.status,
          health: value.health,
        }),
      'Failed to observe deployment',
      true,
    );
  }

  verifySelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.deploymentApi.verify(id), 'Failed to verify deployment', true);
  }

  loadList(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    const projectId = this.form.controls.projectId.value || undefined;
    this.loading.set(true);
    this.error.set(null);
    this.deploymentApi.list(projectId).subscribe({
      next: (items) => {
        this.deployments.set(items);
        if (items.length > 0 && !this.selected()) {
          this.selected.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load deployments');
      },
    });
  }

  loadHistory(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRead() || this.loading()) {
      return;
    }
    this.runSingle(() => this.deploymentApi.history(id), 'Failed to load history', false);
  }

  loadEnvironments(): void {
    this.deploymentApi.environments().subscribe({
      next: (items) => this.environments.set(items),
      error: () => this.environments.set([]),
    });
  }

  selectDeployment(item: Deployment): void {
    this.selected.set(item);
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
    call: () => ReturnType<DeploymentService['observe']>,
    fallback: string,
    refreshList: boolean,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    call().subscribe({
      next: (deployment) => {
        this.selected.set(deployment);
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
