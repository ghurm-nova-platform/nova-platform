import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ExecutionPermissionHelper } from '../execution/execution-permission.helper';
import { ExecutionService } from '../execution/execution.service';
import { ExecutionStatus } from '../execution/execution.models';
import { Conversation, ConversationMessage } from './conversation.models';
import { ConversationPermissionHelper } from './conversation-permission.helper';
import { ConversationService } from './conversation.service';

@Component({
  selector: 'app-conversation-detail-page',
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
  templateUrl: './conversation-detail-page.html',
  styleUrl: './conversation-detail-page.scss',
})
export class ConversationDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly conversationsApi = inject(ConversationService);
  private readonly executionsApi = inject(ExecutionService);
  readonly permissions = inject(ConversationPermissionHelper);
  readonly executionPermissions = inject(ExecutionPermissionHelper);

  readonly projectId = signal('');
  readonly conversationId = signal('');
  readonly conversation = signal<Conversation | null>(null);
  readonly messages = signal<ConversationMessage[]>([]);
  readonly loading = signal(true);
  readonly messagesLoading = signal(false);
  readonly sending = signal(false);
  readonly renaming = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly showRename = signal(false);
  readonly runningExecutionId = signal<string | null>(null);
  readonly runningStatus = signal<ExecutionStatus | null>(null);

  readonly messageControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required],
  });
  readonly titleControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(255)],
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.conversationId.set(this.route.snapshot.paramMap.get('conversationId') ?? '');
    this.reload();
  }

  reload(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.conversationsApi.get(this.projectId(), this.conversationId()).subscribe({
      next: (conversation) => {
        this.conversation.set(conversation);
        this.titleControl.setValue(conversation.title);
        this.loading.set(false);
        this.loadMessages();
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load conversation.');
      },
    });
  }

  loadMessages(): void {
    if (!this.permissions.canReadMessages()) {
      this.messages.set([]);
      return;
    }
    this.messagesLoading.set(true);
    this.conversationsApi
      .listMessages(this.projectId(), this.conversationId(), {
        page: 0,
        size: 100,
        sort: 'sequenceNumber,asc',
      })
      .subscribe({
        next: (page) => {
          this.messages.set(page.content);
          this.messagesLoading.set(false);
        },
        error: () => {
          this.messagesLoading.set(false);
          this.error.set('Unable to load messages.');
        },
      });
  }

  send(): void {
    const current = this.conversation();
    if (
      !current ||
      current.status !== 'ACTIVE' ||
      !this.executionPermissions.canExecute() ||
      !this.permissions.canCreateMessage() ||
      this.messageControl.invalid ||
      this.sending()
    ) {
      this.messageControl.markAsTouched();
      return;
    }

    const content = this.messageControl.value.trim();
    const clientRequestId = crypto.randomUUID();
    this.sending.set(true);
    this.error.set(null);
    this.runningExecutionId.set(null);
    this.runningStatus.set(null);

    this.executionsApi
      .execute(this.projectId(), current.agentId, {
        input: { message: content },
        conversationId: current.id,
        clientRequestId,
      })
      .subscribe({
        next: (result) => {
          this.messageControl.reset();
          this.sending.set(false);
          this.runningExecutionId.set(result.executionId);
          this.runningStatus.set(result.status);
          if (this.isCancellable(result.status)) {
            return;
          }
          this.runningExecutionId.set(null);
          this.runningStatus.set(null);
          this.reload();
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.sending.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to send message.');
        },
      });
  }

  cancelRunning(): void {
    const executionId = this.runningExecutionId();
    if (!executionId || !this.executionPermissions.canCancel()) {
      return;
    }
    if (!window.confirm('Cancel the running execution?')) {
      return;
    }
    this.executionsApi.cancel(this.projectId(), executionId).subscribe({
      next: (updated) => {
        this.runningStatus.set(updated.status);
        if (!this.isCancellable(updated.status)) {
          this.runningExecutionId.set(null);
          this.runningStatus.set(null);
          this.reload();
        }
      },
      error: () => this.error.set('Unable to cancel execution.'),
    });
  }

  startRename(): void {
    const current = this.conversation();
    if (!current || !this.permissions.canUpdate()) {
      return;
    }
    this.titleControl.setValue(current.title);
    this.showRename.set(true);
  }

  saveRename(): void {
    const current = this.conversation();
    if (!current || this.titleControl.invalid || this.renaming()) {
      this.titleControl.markAsTouched();
      return;
    }
    this.renaming.set(true);
    this.error.set(null);
    this.conversationsApi
      .update(this.projectId(), current.id, {
        title: this.titleControl.value.trim(),
        version: current.version,
      })
      .subscribe({
        next: (updated) => {
          this.conversation.set(updated);
          this.showRename.set(false);
          this.renaming.set(false);
        },
        error: (err: { error?: { message?: string } }) => {
          this.renaming.set(false);
          this.error.set(err.error?.message ?? 'Unable to rename conversation.');
        },
      });
  }

  archive(): void {
    const current = this.conversation();
    if (!current || !this.permissions.canArchive() || !window.confirm(`Archive "${current.title}"?`)) {
      return;
    }
    this.conversationsApi.archive(this.projectId(), current.id).subscribe({
      next: (updated) => this.conversation.set(updated),
      error: () => this.error.set('Unable to archive conversation.'),
    });
  }

  restore(): void {
    const current = this.conversation();
    if (!current || !this.permissions.canArchive() || !window.confirm(`Restore "${current.title}"?`)) {
      return;
    }
    this.conversationsApi.restore(this.projectId(), current.id).subscribe({
      next: (updated) => this.conversation.set(updated),
      error: () => this.error.set('Unable to restore conversation.'),
    });
  }

  messageClass(role: ConversationMessage['role']): string {
    return `timeline__message timeline__message--${role.toLowerCase()}`;
  }

  statusClass(status: Conversation['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  isCancellable(status: ExecutionStatus | null): boolean {
    return status === 'PENDING' || status === 'RUNNING';
  }
}
