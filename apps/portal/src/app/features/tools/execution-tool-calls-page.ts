import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { ExecutionToolCall, ToolCallStatus } from './tool.models';
import { ToolPermissionHelper } from './tool-permission.helper';
import { ToolService } from './tool.service';

const POLLING_STATUSES: ToolCallStatus[] = [
  'REQUESTED',
  'APPROVAL_REQUIRED',
  'APPROVED',
  'RUNNING',
];

@Component({
  selector: 'app-execution-tool-calls-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './execution-tool-calls-page.html',
  styleUrl: './execution-tool-calls-page.scss',
})
export class ExecutionToolCallsPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly toolsApi = inject(ToolService);
  readonly permissions = inject(ToolPermissionHelper);

  readonly projectId = signal('');
  readonly executionId = signal('');
  readonly loading = signal(false);
  readonly acting = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly toolCalls = signal<ExecutionToolCall[]>([]);
  readonly continueMessage = signal<string | null>(null);
  readonly rejectReasonControl = new FormControl('USER_REJECTED', { nonNullable: true });
  private pollSub: Subscription | null = null;

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.executionId.set(this.route.snapshot.paramMap.get('executionId') ?? '');
    this.load(true);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  load(startPolling = false): void {
    if (!this.permissions.canReadToolCalls()) {
      this.unauthorized.set(true);
      this.toolCalls.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.toolsApi.listExecutionToolCalls(this.projectId(), this.executionId()).subscribe({
      next: (rows) => {
        this.toolCalls.set(rows);
        this.loading.set(false);
        if (startPolling && this.shouldPoll(rows)) {
          this.startPolling();
        } else if (!this.shouldPoll(rows)) {
          this.stopPolling();
        }
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load tool calls.');
      },
    });
  }

  approve(toolCall: ExecutionToolCall): void {
    if (!this.permissions.canApproveToolCalls() || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.toolsApi
      .approveToolCall(this.projectId(), this.executionId(), toolCall.id, { version: 0 })
      .subscribe({
        next: () => {
          this.acting.set(false);
          this.load(true);
        },
        error: (err: { error?: { message?: string } }) => {
          this.acting.set(false);
          this.error.set(err.error?.message ?? 'Unable to approve tool call.');
        },
      });
  }

  reject(toolCall: ExecutionToolCall): void {
    if (!this.permissions.canApproveToolCalls() || this.acting()) {
      return;
    }
    const reasonCode = this.rejectReasonControl.value.trim() || 'USER_REJECTED';
    this.acting.set(true);
    this.error.set(null);
    this.toolsApi
      .rejectToolCall(this.projectId(), this.executionId(), toolCall.id, {
        version: 0,
        reasonCode,
      })
      .subscribe({
        next: () => {
          this.acting.set(false);
          this.load(true);
        },
        error: (err: { error?: { message?: string } }) => {
          this.acting.set(false);
          this.error.set(err.error?.message ?? 'Unable to reject tool call.');
        },
      });
  }

  continueExecution(): void {
    if (!this.permissions.canExecute() || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.continueMessage.set(null);
    this.toolsApi.continueExecution(this.projectId(), this.executionId()).subscribe({
      next: (response) => {
        this.acting.set(false);
        this.continueMessage.set(response.message);
        this.load(true);
      },
      error: (err: { error?: { message?: string } }) => {
        this.acting.set(false);
        this.error.set(err.error?.message ?? 'Unable to continue execution.');
      },
    });
  }

  hasPendingApproval(): boolean {
    return this.toolCalls().some((row) => row.status === 'APPROVAL_REQUIRED');
  }

  statusClass(status: ToolCallStatus): string {
    return `status status--${status.toLowerCase().replace(/_/g, '-')}`;
  }

  private shouldPoll(rows: ExecutionToolCall[]): boolean {
    return rows.some((row) => POLLING_STATUSES.includes(row.status));
  }

  private startPolling(): void {
    if (this.pollSub) {
      return;
    }
    this.pollSub = timer(3000, 3000)
      .pipe(switchMap(() => this.toolsApi.listExecutionToolCalls(this.projectId(), this.executionId())))
      .subscribe({
        next: (rows) => {
          this.toolCalls.set(rows);
          if (!this.shouldPoll(rows)) {
            this.stopPolling();
          }
        },
        error: () => this.stopPolling(),
      });
  }

  private stopPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }
}
