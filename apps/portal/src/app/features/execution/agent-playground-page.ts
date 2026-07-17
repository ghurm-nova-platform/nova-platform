import { DatePipe, KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';

import { Agent } from '../agents/agent.models';
import { AgentService } from '../agents/agent.service';
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

@Component({
  selector: 'app-agent-playground-page',
  imports: [
    DatePipe,
    KeyValuePipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './agent-playground-page.html',
  styleUrl: './agent-playground-page.scss',
})
export class AgentPlaygroundPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly agentsApi = inject(AgentService);
  private readonly executionsApi = inject(ExecutionService);
  readonly permissions = inject(ExecutionPermissionHelper);

  readonly projectId = signal('');
  readonly agentId = signal('');
  readonly agent = signal<Agent | null>(null);
  readonly loadingAgent = signal(true);
  readonly unauthorized = signal(false);
  readonly error = signal<string | null>(null);

  readonly messageControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required],
  });
  readonly variablesForm = new FormArray<FormGroup<VariableRow>>([]);

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

  run(): void {
    if (!this.permissions.canExecute()) {
      this.unauthorized.set(true);
      return;
    }
    if (this.messageControl.invalid) {
      this.messageControl.markAsTouched();
      return;
    }

    const variables = this.buildVariablesMap();
    this.running.set(true);
    this.error.set(null);
    this.lastResult.set(null);
    this.selectedExecution.set(null);

    this.executionsApi
      .execute(this.projectId(), this.agentId(), {
        input: { message: this.messageControl.value.trim() },
        variables: Object.keys(variables).length > 0 ? variables : undefined,
      })
      .subscribe({
        next: (result) => {
          this.lastResult.set(result);
          this.running.set(false);
          this.loadHistory();
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
        this.historyRows.update((rows) =>
          rows.map((row) => (row.id === updated.id ? updated : row)),
        );
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

  statusClass(status: ExecutionStatus): string {
    return `status status--${status.toLowerCase()}`;
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
