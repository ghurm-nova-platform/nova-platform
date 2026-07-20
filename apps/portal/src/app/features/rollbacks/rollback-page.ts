import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { RollbackPermissionHelper } from './rollback-permission.helper';
import { RollbackService } from './rollback.service';
import { Rollback, RollbackStrategy } from './rollback.models';

@Component({
  selector: 'app-rollback-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './rollback-page.html',
  styleUrl: './rollback-page.scss',
})
export class RollbackPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly rollbackApi = inject(RollbackService);
  readonly permissions = inject(RollbackPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly selected = signal<Rollback | null>(null);
  readonly rollbacks = signal<Rollback[]>([]);

  readonly form = this.fb.nonNullable.group({
    projectId: [''],
    releaseId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    deploymentId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    targetReleaseId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    environment: ['STAGING', Validators.required],
    strategy: ['PREVIOUS_RELEASE' as RollbackStrategy, Validators.required],
    reason: ['', Validators.maxLength(2000)],
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
        this.rollbackApi.create({
          releaseId: value.releaseId,
          deploymentId: value.deploymentId,
          targetReleaseId: value.targetReleaseId,
          environment: value.environment,
          strategy: value.strategy,
          reason: value.reason || undefined,
          riskLevel: 'MEDIUM',
        }),
      'Failed to create rollback plan',
      true,
    );
  }

  validateSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.rollbackApi.validate(id), 'Failed to validate rollback plan', true);
  }

  loadList(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    const projectId = this.form.controls.projectId.value || undefined;
    this.loading.set(true);
    this.error.set(null);
    this.rollbackApi.list(projectId).subscribe({
      next: (items) => {
        this.rollbacks.set(items);
        if (items.length > 0 && !this.selected()) {
          this.selected.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load rollbacks');
      },
    });
  }

  loadHistory(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRead() || this.loading()) {
      return;
    }
    this.runSingle(() => this.rollbackApi.history(id), 'Failed to load history', false);
  }

  selectRollback(item: Rollback): void {
    this.selected.set(item);
  }

  private runSingle(
    call: () => ReturnType<RollbackService['create']>,
    fallback: string,
    refreshList: boolean,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    call().subscribe({
      next: (rollback) => {
        this.selected.set(rollback);
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
