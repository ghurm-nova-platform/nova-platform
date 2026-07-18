import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PatchPermissionHelper } from './patch-permission.helper';
import { PatchService } from './patch.service';
import { PatchResult } from './patch.models';

@Component({
  selector: 'app-patch-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './patch-page.html',
  styleUrl: './patch-page.scss',
})
export class PatchPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly patchApi = inject(PatchService);
  readonly permissions = inject(PatchPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<PatchResult | null>(null);
  readonly expanded = signal<Record<string, boolean>>({});

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  runPatch(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.patchApi.run(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.expanded.set({});
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to run patch agent');
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
    this.patchApi.getLatest(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.expanded.set({});
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load patch result');
      },
    });
  }

  toggleFile(id: string): void {
    this.expanded.update((state) => ({ ...state, [id]: !state[id] }));
  }

  downloadPatch(): void {
    const patch = this.result();
    if (!patch) {
      return;
    }
    const blob = new Blob([patch.patch], { type: 'text/x-diff;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `task-${patch.taskId}.patch`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
