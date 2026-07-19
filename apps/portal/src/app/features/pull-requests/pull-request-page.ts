import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PullRequestPermissionHelper } from './pull-request-permission.helper';
import { PullRequestService } from './pull-request.service';
import { PullRequestOperation } from './pull-request.models';

@Component({
  selector: 'app-pull-request-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './pull-request-page.html',
  styleUrl: './pull-request-page.scss',
})
export class PullRequestPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly pullRequestApi = inject(PullRequestService);
  readonly permissions = inject(PullRequestPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<PullRequestOperation | null>(null);
  readonly history = signal<PullRequestOperation[]>([]);
  readonly copied = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  runPullRequest(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.pullRequestApi.run(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to run pull request agent');
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
    this.pullRequestApi.getLatest(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load pull request operation');
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
    this.pullRequestApi.getHistory(this.form.controls.taskId.value.trim()).subscribe({
      next: (items) => {
        this.history.set(items);
        if (items.length > 0) {
          this.result.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load pull request history');
      },
    });
  }

  repositoryLabel(op: PullRequestOperation): string {
    return `${op.repositoryOwner}/${op.repositoryName}`;
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
}
