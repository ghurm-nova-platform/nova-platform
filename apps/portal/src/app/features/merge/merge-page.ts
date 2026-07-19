import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MergePermissionHelper } from './merge-permission.helper';
import { MergeService } from './merge.service';
import { MergeOperation, MergeValidation } from './merge.models';

@Component({
  selector: 'app-merge-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './merge-page.html',
  styleUrl: './merge-page.scss',
})
export class MergePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly mergeApi = inject(MergeService);
  readonly permissions = inject(MergePermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<MergeOperation | null>(null);
  readonly history = signal<MergeOperation[]>([]);
  readonly copied = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  readonly passedValidations = computed(() =>
    (this.result()?.validations ?? []).filter((check) => check.result === 'PASSED'),
  );

  readonly failedValidations = computed(() =>
    (this.result()?.validations ?? []).filter((check) => check.result === 'FAILED'),
  );

  readonly skippedValidations = computed(() =>
    (this.result()?.validations ?? []).filter(
      (check) => check.result === 'SKIPPED' || check.result === 'ERROR',
    ),
  );

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  runMerge(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.runLoad(() => this.mergeApi.run(this.taskId()), 'Failed to run merge agent');
  }

  loadLatest(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.runLoad(
      () => this.mergeApi.getLatest(this.taskId()),
      'Failed to load latest merge operation',
    );
  }

  loadHistory(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.mergeApi.getHistory(this.taskId()).subscribe({
      next: (items) => {
        this.history.set(items);
        if (items.length > 0) {
          this.result.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load merge history');
      },
    });
  }

  mergeEligibilityLabel(operation: MergeOperation): string {
    if (operation.eligibleForMerge === true) {
      return 'Eligible for merge';
    }
    if (operation.eligibleForMerge === false) {
      return 'Not eligible for merge';
    }
    if (operation.status === 'SUCCEEDED') {
      return 'Merge completed';
    }
    if (operation.status === 'FAILED') {
      return 'Merge blocked or failed';
    }
    return 'Eligibility pending validation';
  }

  mergeEligibilityEligible(operation: MergeOperation): boolean | null {
    if (operation.eligibleForMerge !== null) {
      return operation.eligibleForMerge;
    }
    return operation.status === 'SUCCEEDED' ? true : null;
  }

  maskedFingerprint(fingerprint: string | null | undefined): string {
    if (!fingerprint) {
      return '—';
    }
    if (fingerprint.length <= 12) {
      return fingerprint;
    }
    return `${fingerprint.slice(0, 8)}…${fingerprint.slice(-4)}`;
  }

  safeExternalUrl(url: string | null | undefined): string | null {
    if (!url) {
      return null;
    }
    try {
      const parsed = new URL(url);
      if (parsed.protocol === 'https:' || parsed.protocol === 'http:') {
        return parsed.toString();
      }
    } catch {
      return null;
    }
    return null;
  }

  async copy(value: string | null | undefined, label: string): Promise<void> {
    if (!value) {
      return;
    }
    try {
      await navigator.clipboard.writeText(value);
      this.copied.set(label);
      setTimeout(() => this.copied.set(null), 1500);
    } catch {
      this.error.set('Unable to copy to clipboard');
    }
  }

  validationTrack(_index: number, check: MergeValidation): string {
    return check.id;
  }

  private taskId(): string {
    return this.form.controls.taskId.value.trim();
  }

  private runLoad(
    request: () => ReturnType<MergeService['getLatest']>,
    fallbackMessage: string,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    request().subscribe({
      next: (operation) => {
        this.result.set(operation);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? fallbackMessage);
      },
    });
  }
}
