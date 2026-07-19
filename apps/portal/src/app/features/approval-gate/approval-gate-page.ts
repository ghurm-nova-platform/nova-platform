import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ApprovalGatePermissionHelper } from './approval-gate-permission.helper';
import { ApprovalGateService } from './approval-gate.service';
import {
  ApprovalDecision,
  ApprovalDecisionValue,
  ApprovalHumanActionView,
  ApprovalRequirement,
} from './approval-gate.models';

@Component({
  selector: 'app-approval-gate-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './approval-gate-page.html',
  styleUrl: './approval-gate-page.scss',
})
export class ApprovalGatePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly approvalApi = inject(ApprovalGateService);
  readonly permissions = inject(ApprovalGatePermissionHelper);

  readonly loading = signal(false);
  readonly actionLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<ApprovalDecision | null>(null);
  readonly history = signal<ApprovalDecision[]>([]);
  readonly copied = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  readonly actionForm = this.fb.nonNullable.group({
    comment: [''],
  });

  readonly passedRequirements = computed(() =>
    (this.result()?.requirements ?? []).filter((req) => req.result === 'PASSED'),
  );

  readonly failedRequirements = computed(() =>
    (this.result()?.requirements ?? []).filter((req) => req.result === 'FAILED'),
  );

  readonly pendingRequirements = computed(() =>
    (this.result()?.requirements ?? []).filter(
      (req) => req.result === 'PENDING' || req.result === 'ERROR',
    ),
  );

  readonly approvers = computed(() =>
    (this.result()?.humanActions ?? []).filter((action) => action.action === 'APPROVE'),
  );

  readonly rejections = computed(() =>
    (this.result()?.humanActions ?? []).filter((action) => action.action === 'REJECT'),
  );

  ngOnInit(): void {
    if (
      !this.permissions.canRead() &&
      !this.permissions.canRun() &&
      !this.permissions.canApprove() &&
      !this.permissions.canReject()
    ) {
      this.unauthorized.set(true);
    }
  }

  evaluate(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.runLoad(() => this.approvalApi.run(this.taskId()), 'Failed to evaluate approval gate');
  }

  loadLatest(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.runLoad(
      () => this.approvalApi.getLatest(this.taskId()),
      'Failed to load latest approval decision',
    );
  }

  loadHistory(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.approvalApi.getHistory(this.taskId()).subscribe({
      next: (items) => {
        this.history.set(items);
        if (items.length > 0) {
          this.result.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load approval history');
      },
    });
  }

  approve(): void {
    if (!this.canPerformHumanAction() || this.actionLoading()) {
      return;
    }
    this.actionLoading.set(true);
    this.error.set(null);
    const comment = this.actionForm.controls.comment.value.trim();
    this.approvalApi
      .approve(this.taskId(), { comment: comment || null })
      .subscribe({
        next: (decision) => {
          this.result.set(decision);
          this.actionForm.controls.comment.setValue('');
          this.actionLoading.set(false);
        },
        error: (err) => {
          this.actionLoading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to record approval');
        },
      });
  }

  reject(): void {
    if (!this.canPerformHumanAction() || this.actionLoading()) {
      return;
    }
    const comment = this.actionForm.controls.comment.value.trim();
    if (!comment) {
      this.error.set('Rejection comment is required');
      this.actionForm.controls.comment.markAsTouched();
      return;
    }
    this.actionLoading.set(true);
    this.error.set(null);
    this.approvalApi.reject(this.taskId(), { comment }).subscribe({
      next: (decision) => {
        this.result.set(decision);
        this.actionForm.controls.comment.setValue('');
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to record rejection');
      },
    });
  }

  canPerformHumanAction(): boolean {
    const decision = this.result();
    if (!decision || this.actionsDisabled(decision)) {
      return false;
    }
    return decision.decision === 'REQUIRES_HUMAN_APPROVAL' || decision.decision === 'ELIGIBLE';
  }

  actionsDisabled(decision: ApprovalDecision): boolean {
    const terminal: ApprovalDecisionValue[] = [
      'SUPERSEDED',
      'INVALIDATED',
      'EXPIRED',
      'APPROVED',
      'REJECTED',
      'BLOCKED',
      'ERROR',
    ];
    return decision.stale || terminal.includes(decision.decision);
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

  actorLabel(action: ApprovalHumanActionView): string {
    return action.actorDisplayName ?? action.actorUserId;
  }

  formatValidity(validUntil: string | null | undefined): string {
    if (!validUntil) {
      return 'No expiry configured';
    }
    return validUntil;
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

  requirementTrack(_index: number, req: ApprovalRequirement): string {
    return req.id;
  }

  private taskId(): string {
    return this.form.controls.taskId.value.trim();
  }

  private runLoad(
    request: () => ReturnType<ApprovalGateService['getLatest']>,
    fallbackMessage: string,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    request().subscribe({
      next: (decision) => {
        this.result.set(decision);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? fallbackMessage);
      },
    });
  }
}
