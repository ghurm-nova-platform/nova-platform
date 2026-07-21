import { DatePipe, JsonPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { EMPTY, interval, switchMap, startWith } from 'rxjs';

import { CollaborationPermissionHelper } from './collaboration-permission.helper';
import { CollaborationService } from './collaboration.service';
import {
  CollaborationTimelineEventType,
  DecisionView,
  MessageView,
  SessionDetail,
  SessionSummary,
  TimelineEventView,
} from './collaboration.models';

@Component({
  selector: 'app-collaboration-page',
  imports: [
    DatePipe,
    JsonPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTabsModule,
  ],
  templateUrl: './collaboration-page.html',
  styleUrl: './collaboration-page.scss',
})
export class CollaborationPage implements OnInit {
  private readonly collaborationApi = inject(CollaborationService);
  private readonly destroyRef = inject(DestroyRef);
  readonly permissions = inject(CollaborationPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly pollingSeconds = signal(10);
  readonly sessions = signal<SessionSummary[]>([]);
  readonly selectedSessionId = signal<string | null>(null);
  readonly sessionDetail = signal<SessionDetail | null>(null);

  constructor() {
    toObservable(this.selectedSessionId)
      .pipe(
        switchMap((sessionId) => {
          if (!sessionId) {
            return EMPTY;
          }
          return interval(Math.max(this.pollingSeconds(), 5) * 1000).pipe(
            startWith(0),
            switchMap(() => this.collaborationApi.get(sessionId)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (detail) => {
          this.sessionDetail.set(detail);
          this.error.set(null);
        },
        error: (err) => {
          this.error.set(err?.error?.message ?? 'Failed to load collaboration session');
        },
      });
  }

  readonly selectedSession = computed(() => {
    const id = this.selectedSessionId();
    if (!id) {
      return null;
    }
    return this.sessions().find((session) => session.id === id) ?? null;
  });

  readonly conflictEvents = computed(() => {
    const detail = this.sessionDetail();
    if (!detail) {
      return [];
    }
    return detail.timeline.filter((event) => event.eventType === 'CONFLICT');
  });

  readonly humanRequests = computed(() => {
    const detail = this.sessionDetail();
    if (!detail) {
      return { messages: [] as MessageView[], decisions: [] as DecisionView[] };
    }
    const requestDecisionTypes = new Set([
      'REQUEST_REVIEW',
      'REQUEST_APPROVAL',
      'REQUEST_CLARIFICATION',
    ]);
    return {
      messages: detail.messages.filter((message) => message.messageType === 'APPROVAL_REQUEST'),
      decisions: detail.decisions.filter((decision) =>
        requestDecisionTypes.has(decision.decisionType),
      ),
    };
  });

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      return;
    }

    this.collaborationApi.getConfig().subscribe({
      next: (config) => this.pollingSeconds.set(config.pollingSeconds || 10),
      error: () => this.pollingSeconds.set(10),
    });

    this.loadSessions();
  }

  selectSession(session: SessionSummary): void {
    this.selectedSessionId.set(session.id);
    this.sessionDetail.set(null);
  }

  refreshSessions(): void {
    this.loadSessions();
  }

  refreshSelected(): void {
    const id = this.selectedSessionId();
    if (!id) {
      return;
    }
    this.loadSessionDetail(id, true);
  }

  pauseSelected(): void {
    this.runSessionAction((id) => this.collaborationApi.pause(id), 'Failed to pause session');
  }

  resumeSelected(): void {
    this.runSessionAction((id) => this.collaborationApi.resume(id), 'Failed to resume session');
  }

  cancelSelected(): void {
    this.runSessionAction((id) => this.collaborationApi.cancel(id), 'Failed to cancel session');
  }

  timelineStepLabel(eventType: CollaborationTimelineEventType): string {
    switch (eventType) {
      case 'STARTED':
        return 'Agent';
      case 'TASK_ASSIGNED':
      case 'TASK_STARTED':
      case 'TASK_COMPLETED':
      case 'TASK_BLOCKED':
      case 'TASK_RESUMED':
        return 'Task';
      case 'DECISION':
        return 'Decision';
      case 'MESSAGE_SENT':
        return 'Message';
      case 'APPROVAL':
        return 'Approval';
      case 'COMPLETED':
        return 'Completion';
      case 'CONFLICT':
        return 'Conflict';
      case 'FAILED':
        return 'Failed';
      case 'CANCELLED':
        return 'Cancelled';
      case 'PAUSED':
      case 'RESUMED':
        return 'Session';
      default:
        return 'Event';
    }
  }

  timelineIcon(eventType: CollaborationTimelineEventType): string {
    switch (eventType) {
      case 'STARTED':
        return 'smart_toy';
      case 'TASK_ASSIGNED':
      case 'TASK_STARTED':
      case 'TASK_COMPLETED':
      case 'TASK_BLOCKED':
      case 'TASK_RESUMED':
        return 'task_alt';
      case 'DECISION':
        return 'gavel';
      case 'MESSAGE_SENT':
        return 'chat';
      case 'APPROVAL':
        return 'verified_user';
      case 'COMPLETED':
        return 'check_circle';
      case 'CONFLICT':
        return 'warning';
      case 'FAILED':
        return 'error';
      case 'CANCELLED':
        return 'cancel';
      case 'PAUSED':
      case 'RESUMED':
        return 'pause_circle';
      default:
        return 'radio_button_unchecked';
    }
  }

  statusClass(status: string): string {
    return `collaboration__status collaboration__status--${status.toLowerCase()}`;
  }

  private loadSessions(): void {
    this.loading.set(true);
    this.error.set(null);
    this.collaborationApi.list().subscribe({
      next: (items) => {
        this.sessions.set(items);
        if (items.length > 0 && !this.selectedSessionId()) {
          this.selectSession(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load collaboration sessions');
      },
    });
  }

  private loadSessionDetail(id: string, showLoading: boolean): void {
    if (showLoading) {
      this.loading.set(true);
    }
    this.collaborationApi.get(id).subscribe({
      next: (detail) => {
        this.sessionDetail.set(detail);
        this.loading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load collaboration session');
      },
    });
  }

  private runSessionAction(
    call: (id: string) => ReturnType<CollaborationService['pause']>,
    fallback: string,
  ): void {
    const id = this.selectedSessionId();
    if (!id || !this.permissions.canWrite() || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    call(id).subscribe({
      next: (detail) => {
        this.sessionDetail.set(detail);
        this.loading.set(false);
        this.loadSessions();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? fallback);
      },
    });
  }
}

export function sortTimelineEvents(events: TimelineEventView[]): TimelineEventView[] {
  return [...events].sort(
    (left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime(),
  );
}
