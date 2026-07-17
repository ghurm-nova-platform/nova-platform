import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Agent, AgentStatus } from './agent.models';
import { AgentPermissionHelper } from './agent-permission.helper';
import { AgentService } from './agent.service';

@Component({
  selector: 'app-agent-detail-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './agent-detail-page.html',
  styleUrl: './agent-detail-page.scss',
})
export class AgentDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agentsApi = inject(AgentService);
  readonly permissions = inject(AgentPermissionHelper);

  readonly projectId = signal('');
  readonly agentId = signal('');
  readonly agent = signal<Agent | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.agentId.set(this.route.snapshot.paramMap.get('agentId') ?? '');
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.agentsApi.get(this.projectId(), this.agentId()).subscribe({
      next: (agent) => {
        this.agent.set(agent);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load agent details.');
        this.loading.set(false);
      },
    });
  }

  edit(): void {
    void this.router.navigate(['/projects', this.projectId(), 'agents', this.agentId(), 'edit']);
  }

  setStatus(status: AgentStatus): void {
    const current = this.agent();
    if (!current) {
      return;
    }
    const label = status.toLowerCase();
    if (!window.confirm(`${label[0].toUpperCase()}${label.slice(1)} agent "${current.name}"?`)) {
      return;
    }
    this.agentsApi
      .updateStatus(this.projectId(), this.agentId(), { status, version: current.version })
      .subscribe({
        next: (agent) => this.agent.set(agent),
        error: (err: { error?: { message?: string } }) =>
          this.error.set(err.error?.message ?? 'Unable to update status.'),
      });
  }

  archive(): void {
    const current = this.agent();
    if (!current || !window.confirm(`Archive agent "${current.name}"?`)) {
      return;
    }
    this.agentsApi.archive(this.projectId(), this.agentId()).subscribe({
      next: () => this.reload(),
      error: () => this.error.set('Unable to archive agent.'),
    });
  }

  statusClass(status: AgentStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
