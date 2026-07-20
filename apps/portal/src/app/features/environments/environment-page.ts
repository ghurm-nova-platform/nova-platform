import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { EnvironmentPermissionHelper } from './environment-permission.helper';
import { EnvironmentService } from './environment.service';
import { EnvironmentType, ManagedEnvironment } from './environment.models';

@Component({
  selector: 'app-environment-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './environment-page.html',
  styleUrl: './environment-page.scss',
})
export class EnvironmentPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly environmentApi = inject(EnvironmentService);
  readonly permissions = inject(EnvironmentPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly selected = signal<ManagedEnvironment | null>(null);
  readonly environments = signal<ManagedEnvironment[]>([]);

  readonly form = this.fb.nonNullable.group({
    projectId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', Validators.maxLength(2000)],
    environmentType: ['STAGING' as EnvironmentType, Validators.required],
    region: [''],
    provider: [''],
    clusterName: [''],
    namespaceName: [''],
    ownerName: [''],
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
        this.environmentApi.create({
          projectId: value.projectId,
          name: value.name,
          description: value.description || undefined,
          environmentType: value.environmentType,
          region: value.region || undefined,
          provider: value.provider || undefined,
          clusterName: value.clusterName || undefined,
          namespaceName: value.namespaceName || undefined,
          ownerName: value.ownerName || undefined,
        }),
      'Failed to create environment',
      true,
    );
  }

  updateSelected(): void {
    const env = this.selected();
    if (!env || !this.permissions.canRun() || this.loading()) {
      return;
    }
    const value = this.form.getRawValue();
    this.runSingle(
      () =>
        this.environmentApi.update(env.id, {
          description: value.description || undefined,
          region: value.region || undefined,
          provider: value.provider || undefined,
          clusterName: value.clusterName || undefined,
          namespaceName: value.namespaceName || undefined,
          ownerName: value.ownerName || undefined,
        }),
      'Failed to update environment',
      true,
    );
  }

  enableSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.environmentApi.enable(id), 'Failed to enable environment', true);
  }

  disableSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.environmentApi.disable(id), 'Failed to disable environment', true);
  }

  archiveSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.environmentApi.archive(id), 'Failed to archive environment', true);
  }

  loadList(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    const projectId = this.form.controls.projectId.value;
    if (!projectId) {
      this.form.controls.projectId.markAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.environmentApi.list(projectId).subscribe({
      next: (items) => {
        this.environments.set(items);
        if (items.length > 0 && !this.selected()) {
          this.selected.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load environments');
      },
    });
  }

  loadHistory(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRead() || this.loading()) {
      return;
    }
    this.runSingle(() => this.environmentApi.history(id), 'Failed to load history', false);
  }

  selectEnvironment(item: ManagedEnvironment): void {
    this.selected.set(item);
  }

  private runSingle(
    call: () => ReturnType<EnvironmentService['create']>,
    fallback: string,
    refreshList: boolean,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    call().subscribe({
      next: (environment) => {
        this.selected.set(environment);
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
