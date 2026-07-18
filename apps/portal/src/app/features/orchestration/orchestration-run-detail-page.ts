import { DatePipe, DecimalPipe, KeyValuePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { OrchestrationPermissionHelper } from './orchestration-permission.helper';
import { OrchestrationRunService } from './orchestration-run.service';
import {
  ACTIVE_RUN_STATUSES,
  OrchestrationEvent,
  OrchestrationRun,
  OrchestrationTask,
  RunStatus,
  TaskStatus,
} from './orchestration.models';

@Component({
  selector: 'app-orchestration-run-detail-page',
  imports: [
    DatePipe,
    DecimalPipe,
    KeyValuePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './orchestration-run-detail-page.html',
  styleUrl: './orchestration-page.scss',
})
export class OrchestrationRunDetailPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly runsApi = inject(OrchestrationRunService);
  readonly permissions = inject(OrchestrationPermissionHelper);

  readonly runId = signal('');
  readonly loading = signal(true);
  readonly acting = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly run = signal<OrchestrationRun | null>(null);
  readonly tasks = signal<OrchestrationTask[]>([]);
  readonly events = signal<OrchestrationEvent[]>([]);
  readonly taskColumns = ['taskKey', 'displayName', 'taskType', 'status', 'attemptCount', 'priority'];
  private pollSub: Subscription | null = null;

  ngOnInit(): void {
    this.runId.set(this.route.snapshot.paramMap.get('runId') ?? '');
    this.load(true);
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  edit(): void {
    void this.router.navigate(['/orchestration-runs', this.runId(), 'edit']);
  }

  openGraph(): void {
    void this.router.navigate(['/orchestration-runs', this.runId(), 'graph']);
  }

  markReady(): void {
    if (!this.permissions.canStartRun() || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.runsApi.ready(this.runId()).subscribe({
      next: (run) => {
        this.acting.set(false);
        this.applyRun(run);
      },
      error: (err: { error?: { message?: string } }) => {
        this.acting.set(false);
        this.error.set(err.error?.message ?? 'Unable to mark run ready.');
      },
    });
  }

  start(): void {
    if (!this.permissions.canStartRun() || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.runsApi.start(this.runId()).subscribe({
      next: (run) => {
        this.acting.set(false);
        this.applyRun(run);
        this.loadTasks();
        this.loadEvents();
        this.startPolling();
      },
      error: (err: { error?: { message?: string } }) => {
        this.acting.set(false);
        this.error.set(err.error?.message ?? 'Unable to start run.');
      },
    });
  }

  cancel(): void {
    if (!this.permissions.canCancelRun() || this.acting()) {
      return;
    }
    const reason = window.prompt('Cancellation reason (optional):');
    if (reason === null) {
      return;
    }
    if (!window.confirm('Cancel this orchestration run?')) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.runsApi.cancel(this.runId(), { reason: reason.trim() || null }).subscribe({
      next: (run) => {
        this.acting.set(false);
        this.applyRun(run);
        this.loadTasks();
        this.loadEvents();
      },
      error: (err: { error?: { message?: string } }) => {
        this.acting.set(false);
        this.error.set(err.error?.message ?? 'Unable to cancel run.');
      },
    });
  }

  archive(): void {
    if (!this.permissions.canArchiveRun() || this.acting()) {
      return;
    }
    if (!window.confirm('Archive this orchestration run?')) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.runsApi.archive(this.runId()).subscribe({
      next: (run) => {
        this.acting.set(false);
        this.applyRun(run);
      },
      error: (err: { error?: { message?: string } }) => {
        this.acting.set(false);
        this.error.set(err.error?.message ?? 'Unable to archive run.');
      },
    });
  }

  statusClass(status: RunStatus | TaskStatus): string {
    return `status status--${status.toLowerCase().replace(/_/g, '-')}`;
  }

  private load(startPolling = false): void {
    if (!this.permissions.canReadRuns()) {
      this.unauthorized.set(true);
      this.loading.set(false);
      return;
    }
    this.runsApi.get(this.runId()).subscribe({
      next: (run) => {
        this.applyRun(run);
        this.loading.set(false);
        this.loadTasks();
        this.loadEvents();
        if (startPolling && this.isActive(run.status)) {
          this.startPolling();
        } else if (!this.isActive(run.status)) {
          this.stopPolling();
        }
      },
      error: (err: { status?: number }) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.unauthorized.set(true);
          return;
        }
        this.error.set('Unable to load orchestration run.');
      },
    });
  }

  private loadTasks(): void {
    this.runsApi.listTasks(this.runId(), { page: 0, size: 100, sort: 'sequenceOrder,asc' }).subscribe({
      next: (page) => this.tasks.set(page.content),
      error: () => this.tasks.set([]),
    });
  }

  private loadEvents(): void {
    if (!this.permissions.canReadEvents()) {
      this.events.set([]);
      return;
    }
    this.runsApi.listEvents(this.runId(), { page: 0, size: 50, sort: 'eventSequence,desc' }).subscribe({
      next: (page) => this.events.set(page.content),
      error: () => this.events.set([]),
    });
  }

  private applyRun(run: OrchestrationRun): void {
    this.run.set(run);
    if (this.isActive(run.status)) {
      this.startPolling();
    } else {
      this.stopPolling();
    }
  }

  private isActive(status: RunStatus): boolean {
    return ACTIVE_RUN_STATUSES.includes(status);
  }

  private startPolling(): void {
    if (this.pollSub) {
      return;
    }
    this.pollSub = timer(3000, 3000)
      .pipe(switchMap(() => this.runsApi.get(this.runId())))
      .subscribe({
        next: (run) => {
          this.run.set(run);
          this.loadTasks();
          this.loadEvents();
          if (!this.isActive(run.status)) {
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
