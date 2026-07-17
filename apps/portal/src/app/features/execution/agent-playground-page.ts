import { DatePipe, KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { Agent } from '../agents/agent.models';
import { AgentService } from '../agents/agent.service';
import { Conversation, ConversationMessage } from '../conversations/conversation.models';
import { ConversationPermissionHelper } from '../conversations/conversation-permission.helper';
import { ConversationService } from '../conversations/conversation.service';
import { ExecutionPermissionHelper } from './execution-permission.helper';
import {
  AgentExecuteResponse,
  Execution,
  ExecutionStatus,
  ExecutionTokenUsage,
} from './execution.models';
import { ExecutionService } from './execution.service';

interface VariableRow {
  key: FormControl<string>;
  value: FormControl<string>;
}

type PlaygroundMode = 'stateless' | 'conversation';

@Component({
  selector: 'app-agent-playground-page',
  imports: [
    DatePipe,
    KeyValuePipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './agent-playground-page.html',
  styleUrl: './agent-playground-page.scss',
})
export class AgentPlaygroundPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agentsApi = inject(AgentService);
  private readonly executionsApi = inject(ExecutionService);
  private readonly conversationsApi = inject(ConversationService);
  readonly permissions = inject(ExecutionPermissionHelper);
  readonly conversationPermissions = inject(ConversationPermissionHelper);

  readonly projectId = signal('');
  readonly agentId = signal('');
  readonly agent = signal<Agent | null>(null);
  readonly loadingAgent = signal(true);
  readonly unauthorized = signal(false);
  readonly error = signal<string | null>(null);

  readonly mode = signal<PlaygroundMode>('stateless');
  readonly conversations = signal<Conversation[]>([]);
  readonly conversationsLoading = signal(false);
  readonly selectedConversationId = signal<string | null>(null);
  readonly selectedConversation = signal<Conversation | null>(null);
  readonly conversationMessages = signal<ConversationMessage[]>([]);
  readonly conversationMessagesLoading = signal(false);
  readonly creatingConversation = signal(false);
  readonly renamingConversation = signal(false);
  readonly showRenameConversation = signal(false);

  readonly messageControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required],
  });
  readonly variablesForm = new FormArray<FormGroup<VariableRow>>([]);
  readonly conversationTitleControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(255)],
  });
  readonly renameTitleControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(255)],
  });

  readonly running = signal(false);
  readonly lastResult = signal<AgentExecuteResponse | null>(null);
  readonly selectedExecution = signal<Execution | null>(null);
  readonly detailLoading = signal(false);

  readonly historyLoading = signal(false);
  readonly historyError = signal<string | null>(null);
  readonly historyRows = signal<Execution[]>([]);
  readonly historyTotal = signal(0);
  readonly historyPageIndex = signal(0);
  readonly historyPageSize = signal(10);
  readonly historyColumns = ['status', 'latencyMs', 'tokens', 'createdAt', 'actions'];

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.agentId.set(this.route.snapshot.paramMap.get('agentId') ?? '');
    this.loadAgent();
    this.loadHistory();
  }

  get variablesArray(): FormArray<FormGroup<VariableRow>> {
    return this.variablesForm;
  }

  addVariable(key = '', value = ''): void {
    this.variablesArray.push(this.createVariableGroup(key, value));
  }

  removeVariable(index: number): void {
    this.variablesArray.removeAt(index);
  }

  onModeChange(mode: PlaygroundMode): void {
    this.mode.set(mode);
    this.error.set(null);
    if (mode === 'conversation') {
      this.loadConversations();
      return;
    }
    this.selectedConversationId.set(null);
    this.selectedConversation.set(null);
    this.conversationMessages.set([]);
  }

  onConversationChange(conversationId: string): void {
    this.selectedConversationId.set(conversationId || null);
    const conversation = this.conversations().find((row) => row.id === conversationId) ?? null;
    this.selectedConversation.set(conversation);
    this.showRenameConversation.set(false);
    if (conversation) {
      this.loadConversationMessages(conversation.id);
      return;
    }
    this.conversationMessages.set([]);
  }

  createConversation(): void {
    if (!this.conversationPermissions.canCreate() || this.creatingConversation()) {
      return;
    }
    this.creatingConversation.set(true);
    this.error.set(null);
    const title = this.conversationTitleControl.value.trim();
    this.conversationsApi
      .create(this.projectId(), {
        agentId: this.agentId(),
        title: title || undefined,
      })
      .subscribe({
        next: (conversation) => {
          this.conversationTitleControl.reset();
          this.creatingConversation.set(false);
          this.loadConversations(conversation.id);
        },
        error: (err: { error?: { message?: string } }) => {
          this.creatingConversation.set(false);
          this.error.set(err.error?.message ?? 'Unable to create conversation.');
        },
      });
  }

  startRenameConversation(): void {
    const current = this.selectedConversation();
    if (!current || !this.conversationPermissions.canUpdate()) {
      return;
    }
    this.renameTitleControl.setValue(current.title);
    this.showRenameConversation.set(true);
  }

  saveRenameConversation(): void {
    const current = this.selectedConversation();
    if (!current || this.renameTitleControl.invalid || this.renamingConversation()) {
      this.renameTitleControl.markAsTouched();
      return;
    }
    this.renamingConversation.set(true);
    this.conversationsApi
      .update(this.projectId(), current.id, {
        title: this.renameTitleControl.value.trim(),
        version: current.version,
      })
      .subscribe({
        next: (updated) => {
          this.selectedConversation.set(updated);
          this.conversations.update((rows) => rows.map((row) => (row.id === updated.id ? updated : row)));
          this.showRenameConversation.set(false);
          this.renamingConversation.set(false);
        },
        error: (err: { error?: { message?: string } }) => {
          this.renamingConversation.set(false);
          this.error.set(err.error?.message ?? 'Unable to rename conversation.');
        },
      });
  }

  archiveSelectedConversation(): void {
    const current = this.selectedConversation();
    if (!current || !this.conversationPermissions.canArchive() || !window.confirm(`Archive "${current.title}"?`)) {
      return;
    }
    this.conversationsApi.archive(this.projectId(), current.id).subscribe({
      next: (updated) => {
        this.selectedConversation.set(updated);
        this.conversations.update((rows) => rows.map((row) => (row.id === updated.id ? updated : row)));
      },
      error: () => this.error.set('Unable to archive conversation.'),
    });
  }

  restoreSelectedConversation(): void {
    const current = this.selectedConversation();
    if (!current || !this.conversationPermissions.canArchive() || !window.confirm(`Restore "${current.title}"?`)) {
      return;
    }
    this.conversationsApi.restore(this.projectId(), current.id).subscribe({
      next: (updated) => {
        this.selectedConversation.set(updated);
        this.conversations.update((rows) => rows.map((row) => (row.id === updated.id ? updated : row)));
      },
      error: () => this.error.set('Unable to restore conversation.'),
    });
  }

  run(): void {
    if (!this.permissions.canExecute()) {
      this.unauthorized.set(true);
      return;
    }
    if (this.messageControl.invalid) {
      this.messageControl.markAsTouched();
      return;
    }
    if (this.mode() === 'conversation') {
      const conversationId = this.selectedConversationId();
      if (!conversationId) {
        this.error.set('Select or create a conversation before sending.');
        return;
      }
      const conversation = this.selectedConversation();
      if (!conversation || conversation.status !== 'ACTIVE') {
        this.error.set('Select an active conversation before sending.');
        return;
      }
    }

    const variables = this.buildVariablesMap();
    const body: {
      input: { message: string };
      variables?: Record<string, string>;
      conversationId?: string;
      clientRequestId?: string;
    } = {
      input: { message: this.messageControl.value.trim() },
      variables: Object.keys(variables).length > 0 ? variables : undefined,
    };

    if (this.mode() === 'conversation') {
      body.conversationId = this.selectedConversationId() ?? undefined;
      body.clientRequestId = crypto.randomUUID();
    }

    this.running.set(true);
    this.error.set(null);
    this.lastResult.set(null);
    this.selectedExecution.set(null);

    this.executionsApi.execute(this.projectId(), this.agentId(), body).subscribe({
      next: (result) => {
        this.lastResult.set(result);
        this.running.set(false);
        this.loadHistory();
        if (this.mode() === 'conversation' && this.selectedConversationId()) {
          this.reloadSelectedConversation();
        }
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.running.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to execute agent.');
      },
    });
  }

  loadHistory(): void {
    if (!this.permissions.canRead()) {
      return;
    }
    this.historyLoading.set(true);
    this.historyError.set(null);
    this.executionsApi
      .list(this.projectId(), {
        agentId: this.agentId(),
        page: this.historyPageIndex(),
        size: this.historyPageSize(),
        sort: 'createdAt,desc',
      })
      .subscribe({
        next: (page) => {
          this.historyRows.set(page.content);
          this.historyTotal.set(page.totalElements);
          this.historyLoading.set(false);
        },
        error: (err: { status?: number }) => {
          this.historyLoading.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.historyError.set('Unable to load execution history.');
        },
      });
  }

  openDetail(execution: Execution): void {
    this.detailLoading.set(true);
    this.executionsApi.get(this.projectId(), execution.id).subscribe({
      next: (detail) => {
        this.selectedExecution.set(detail);
        this.detailLoading.set(false);
      },
      error: () => {
        this.selectedExecution.set(execution);
        this.detailLoading.set(false);
      },
    });
  }

  cancelExecution(execution: Execution): void {
    if (!this.permissions.canCancel() || !this.isCancellable(execution.status)) {
      return;
    }
    if (!window.confirm('Cancel this execution?')) {
      return;
    }
    this.executionsApi.cancel(this.projectId(), execution.id).subscribe({
      next: (updated) => {
        this.historyRows.update((rows) => rows.map((row) => (row.id === updated.id ? updated : row)));
        if (this.selectedExecution()?.id === updated.id) {
          this.selectedExecution.set(updated);
        }
      },
      error: () => this.historyError.set('Unable to cancel execution.'),
    });
  }

  onHistoryPage(event: PageEvent): void {
    this.historyPageIndex.set(event.pageIndex);
    this.historyPageSize.set(event.pageSize);
    this.loadHistory();
  }

  variablesPreview(): Record<string, string> {
    return this.buildVariablesMap();
  }

  renderedPromptPreview(): string {
    const result = this.lastResult();
    if (result?.renderedPrompt) {
      return result.renderedPrompt;
    }
    const detail = this.selectedExecution();
    if (detail?.renderedPrompt) {
      return detail.renderedPrompt;
    }
    return '';
  }

  responsePreview(): string {
    const result = this.lastResult();
    if (result?.response) {
      return result.response;
    }
    const detail = this.selectedExecution();
    if (detail?.response) {
      return detail.response;
    }
    return '';
  }

  activeStatus(): ExecutionStatus | null {
    return this.lastResult()?.status ?? this.selectedExecution()?.status ?? null;
  }

  activeLatency(): number | null {
    const result = this.lastResult();
    if (result) {
      return result.latencyMs;
    }
    return this.selectedExecution()?.latencyMs ?? null;
  }

  activeTokens(): ExecutionTokenUsage | null {
    const result = this.lastResult();
    if (result?.tokens) {
      return result.tokens;
    }
    return this.selectedExecution()?.tokens ?? null;
  }

  activeError(): string | null {
    const result = this.lastResult();
    if (result?.errorMessage) {
      return result.errorMessage;
    }
    return this.selectedExecution()?.errorMessage ?? null;
  }

  isCancellable(status: ExecutionStatus): boolean {
    return status === 'PENDING' || status === 'RUNNING';
  }

  statusClass(status: ExecutionStatus | Conversation['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  conversationStatusClass(status: Conversation['status']): string {
    return this.statusClass(status);
  }

  messageClass(role: ConversationMessage['role']): string {
    return `conversation-message conversation-message--${role.toLowerCase()}`;
  }

  openConversationDetail(): void {
    const conversationId = this.selectedConversationId();
    if (!conversationId) {
      return;
    }
    void this.router.navigate(['/projects', this.projectId(), 'conversations', conversationId]);
  }

  formatTokens(tokens: ExecutionTokenUsage | null): string {
    if (!tokens) {
      return '—';
    }
    return `${tokens.input} / ${tokens.output} / ${tokens.total}`;
  }

  private loadAgent(): void {
    this.loadingAgent.set(true);
    this.agentsApi.get(this.projectId(), this.agentId()).subscribe({
      next: (agent) => {
        this.agent.set(agent);
        this.loadingAgent.set(false);
      },
      error: () => {
        this.error.set('Unable to load agent.');
        this.loadingAgent.set(false);
      },
    });
  }

  private loadConversations(selectId?: string): void {
    if (!this.conversationPermissions.canRead()) {
      this.conversations.set([]);
      return;
    }
    this.conversationsLoading.set(true);
    this.conversationsApi
      .list(this.projectId(), {
        agentId: this.agentId(),
        status: 'ACTIVE',
        size: 50,
        sort: 'lastMessageAt,desc',
      })
      .subscribe({
        next: (page) => {
          this.conversations.set(page.content);
          this.conversationsLoading.set(false);
          const nextId = selectId ?? this.selectedConversationId() ?? page.content[0]?.id ?? null;
          if (nextId) {
            this.onConversationChange(nextId);
          }
        },
        error: () => {
          this.conversationsLoading.set(false);
          this.error.set('Unable to load conversations.');
        },
      });
  }

  private reloadSelectedConversation(): void {
    const conversationId = this.selectedConversationId();
    if (!conversationId) {
      return;
    }
    this.conversationsApi.get(this.projectId(), conversationId).subscribe({
      next: (conversation) => {
        this.selectedConversation.set(conversation);
        this.conversations.update((rows) => rows.map((row) => (row.id === conversation.id ? conversation : row)));
        this.loadConversationMessages(conversation.id);
      },
      error: () => this.loadConversationMessages(conversationId),
    });
  }

  private loadConversationMessages(conversationId: string): void {
    if (!this.conversationPermissions.canReadMessages()) {
      this.conversationMessages.set([]);
      return;
    }
    this.conversationMessagesLoading.set(true);
    this.conversationsApi
      .listMessages(this.projectId(), conversationId, {
        page: 0,
        size: 20,
        sort: 'sequenceNumber,desc',
      })
      .subscribe({
        next: (page) => {
          this.conversationMessages.set([...page.content].reverse());
          this.conversationMessagesLoading.set(false);
        },
        error: () => {
          this.conversationMessagesLoading.set(false);
          this.error.set('Unable to load conversation messages.');
        },
      });
  }

  private createVariableGroup(key = '', value = ''): FormGroup<VariableRow> {
    return new FormGroup({
      key: new FormControl(key, { nonNullable: true }),
      value: new FormControl(value, { nonNullable: true }),
    });
  }

  private buildVariablesMap(): Record<string, string> {
    const variables: Record<string, string> = {};
    for (const group of this.variablesArray.controls) {
      const key = group.controls.key.value.trim();
      const value = group.controls.value.value;
      if (key) {
        variables[key] = value;
      }
    }
    return variables;
  }
}
