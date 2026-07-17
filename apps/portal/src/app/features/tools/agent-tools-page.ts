import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AgentToolAssignment, ToolDefinition } from './tool.models';
import { ToolPermissionHelper } from './tool-permission.helper';
import { ToolService } from './tool.service';

@Component({
  selector: 'app-agent-tools-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './agent-tools-page.html',
  styleUrl: './agent-tools-page.scss',
})
export class AgentToolsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly toolsApi = inject(ToolService);
  readonly permissions = inject(ToolPermissionHelper);

  readonly projectId = signal('');
  readonly agentId = signal('');
  readonly loading = signal(false);
  readonly assigning = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly assignments = signal<AgentToolAssignment[]>([]);
  readonly availableTools = signal<ToolDefinition[]>([]);
  readonly displayedColumns = ['toolKey', 'toolName', 'toolStatus', 'updatedAt', 'actions'];
  readonly toolControl = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.agentId.set(this.route.snapshot.paramMap.get('agentId') ?? '');
    this.load();
  }

  load(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      this.assignments.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.toolsApi.listAgentTools(this.projectId(), this.agentId()).subscribe({
      next: (rows) => {
        this.assignments.set(rows);
        this.loading.set(false);
        this.loadAvailableTools(rows);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load agent tools.');
      },
    });
  }

  assign(): void {
    if (!this.permissions.canAssign() || this.toolControl.invalid || this.assigning()) {
      this.toolControl.markAsTouched();
      return;
    }
    this.assigning.set(true);
    this.error.set(null);
    this.toolsApi.assignTool(this.projectId(), this.agentId(), { toolId: this.toolControl.value }).subscribe({
      next: () => {
        this.toolControl.reset();
        this.assigning.set(false);
        this.load();
      },
      error: (err: { error?: { message?: string } }) => {
        this.assigning.set(false);
        this.error.set(err.error?.message ?? 'Unable to assign tool.');
      },
    });
  }

  unassign(assignment: AgentToolAssignment): void {
    if (!this.permissions.canAssign() || !window.confirm(`Unassign tool "${assignment.toolName}"?`)) {
      return;
    }
    this.toolsApi.unassignTool(this.projectId(), this.agentId(), assignment.toolId).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to unassign tool.'),
    });
  }

  statusClass(status: AgentToolAssignment['toolStatus']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadAvailableTools(assignments: AgentToolAssignment[]): void {
    if (!this.permissions.canAssign()) {
      this.availableTools.set([]);
      return;
    }
    this.toolsApi.listTools(this.projectId(), { status: 'ACTIVE', size: 100, sort: 'name,asc' }).subscribe({
      next: (page) => {
        const assignedIds = new Set(assignments.map((row) => row.toolId));
        this.availableTools.set(page.content.filter((tool) => !assignedIds.has(tool.id)));
      },
      error: () => this.error.set('Unable to load available tools.'),
    });
  }
}
