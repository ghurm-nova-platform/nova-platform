import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { RepairPermissionHelper } from './repair-permission.helper';
import { RepairService } from './repair.service';
import { RepairOperation } from './repair.models';

@Component({
  selector: 'app-repair-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './repair-page.html',
  styleUrl: './repair-page.scss',
})
export class RepairPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly repairApi = inject(RepairService);
  readonly permissions = inject(RepairPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<RepairOperation | null>(null);
  readonly history = signal<RepairOperation[]>([]);

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  runRepair(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.repairApi.run(this.form.controls.taskId.value.trim()).subscribe({
      next: (operation) => {
        this.result.set(operation);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to run repair agent');
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
    this.repairApi.getLatest(this.form.controls.taskId.value.trim()).subscribe({
      next: (operation) => {
        this.result.set(operation);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load repair operation');
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
    this.repairApi.getHistory(this.form.controls.taskId.value.trim()).subscribe({
      next: (items) => {
        this.history.set(items);
        if (items.length > 0) {
          this.result.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load repair history');
      },
    });
  }

  formatConfidence(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    return `${Math.round(value * 100)}%`;
  }
}
