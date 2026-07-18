import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import {
  AgentKnowledgeAssignment,
  KnowledgeBase,
  KnowledgeBaseStatus,
} from './knowledge.models';
import { KnowledgePermissionHelper } from './knowledge-permission.helper';
import { KnowledgeService } from './knowledge.service';

@Component({
  selector: 'app-agent-knowledge-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './agent-knowledge-page.html',
  styleUrl: './agent-knowledge-page.scss',
})
export class AgentKnowledgePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly knowledgeApi = inject(KnowledgeService);
  readonly permissions = inject(KnowledgePermissionHelper);

  readonly projectId = signal('');
  readonly agentId = signal('');
  readonly loading = signal(false);
  readonly assigning = signal(false);
  readonly updating = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly assignments = signal<AgentKnowledgeAssignment[]>([]);
  readonly availableKnowledgeBases = signal<KnowledgeBase[]>([]);
  readonly displayedColumns = [
    'knowledgeKey',
    'knowledgeBaseName',
    'knowledgeBaseStatus',
    'topKOverride',
    'minimumScoreOverride',
    'updatedAt',
    'actions',
  ];
  readonly assignForm = this.fb.nonNullable.group({
    knowledgeBaseId: ['', Validators.required],
    topKOverride: [''],
    minimumScoreOverride: [''],
  });

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
    this.knowledgeApi.listAgentKnowledgeBases(this.projectId(), this.agentId()).subscribe({
      next: (rows) => {
        this.assignments.set(rows);
        this.loading.set(false);
        this.loadAvailableKnowledgeBases(rows);
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load agent knowledge assignments.');
      },
    });
  }

  assign(): void {
    if (!this.permissions.canAssign() || this.assignForm.invalid || this.assigning()) {
      this.assignForm.markAllAsTouched();
      return;
    }
    this.assigning.set(true);
    this.error.set(null);
    const raw = this.assignForm.getRawValue();
    this.knowledgeApi
      .assignKnowledgeBase(this.projectId(), this.agentId(), {
        knowledgeBaseId: raw.knowledgeBaseId,
        topKOverride: this.parseOptionalNumber(raw.topKOverride),
        minimumScoreOverride: this.parseOptionalNumber(raw.minimumScoreOverride),
      })
      .subscribe({
        next: () => {
          this.assignForm.reset();
          this.assigning.set(false);
          this.load();
        },
        error: (err: { error?: { message?: string } }) => {
          this.assigning.set(false);
          this.error.set(err.error?.message ?? 'Unable to assign knowledge base.');
        },
      });
  }

  updateAssignment(assignment: AgentKnowledgeAssignment): void {
    if (!this.permissions.canAssign() || this.updating()) {
      return;
    }
    const topKRaw = window.prompt('Top K override (leave blank for default)', assignment.topKOverride?.toString() ?? '');
    if (topKRaw === null) {
      return;
    }
    const scoreRaw = window.prompt(
      'Minimum score override (leave blank for default)',
      assignment.minimumScoreOverride?.toString() ?? '',
    );
    if (scoreRaw === null) {
      return;
    }
    this.updating.set(true);
    this.knowledgeApi
      .updateKnowledgeAssignment(this.projectId(), this.agentId(), assignment.knowledgeBaseId, {
        version: assignment.version,
        topKOverride: this.parseOptionalNumber(topKRaw),
        minimumScoreOverride: this.parseOptionalNumber(scoreRaw),
      })
      .subscribe({
        next: () => {
          this.updating.set(false);
          this.load();
        },
        error: () => {
          this.updating.set(false);
          this.error.set('Unable to update assignment.');
        },
      });
  }

  unassign(assignment: AgentKnowledgeAssignment): void {
    if (
      !this.permissions.canAssign() ||
      !window.confirm(`Unassign knowledge base "${assignment.knowledgeBaseName}"?`)
    ) {
      return;
    }
    this.knowledgeApi
      .unassignKnowledgeBase(this.projectId(), this.agentId(), assignment.knowledgeBaseId)
      .subscribe({
        next: () => this.load(),
        error: () => this.error.set('Unable to unassign knowledge base.'),
      });
  }

  statusClass(status: KnowledgeBaseStatus): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadAvailableKnowledgeBases(assignments: AgentKnowledgeAssignment[]): void {
    if (!this.permissions.canAssign()) {
      this.availableKnowledgeBases.set([]);
      return;
    }
    this.knowledgeApi
      .listKnowledgeBases(this.projectId(), { status: 'ACTIVE', size: 100, sort: 'name,asc' })
      .subscribe({
        next: (page) => {
          const assignedIds = new Set(assignments.map((row) => row.knowledgeBaseId));
          this.availableKnowledgeBases.set(page.content.filter((kb) => !assignedIds.has(kb.id)));
        },
        error: () => this.error.set('Unable to load available knowledge bases.'),
      });
  }

  private parseOptionalNumber(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }
}
