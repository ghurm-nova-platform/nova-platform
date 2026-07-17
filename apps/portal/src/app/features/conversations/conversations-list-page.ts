import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import {
  Conversation,
  ConversationStatus,
  CONVERSATION_STATUSES,
} from './conversation.models';
import { ConversationPermissionHelper } from './conversation-permission.helper';
import { ConversationService } from './conversation.service';

@Component({
  selector: 'app-conversations-list-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './conversations-list-page.html',
  styleUrl: './conversations-list-page.scss',
})
export class ConversationsListPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly conversationsApi = inject(ConversationService);
  readonly permissions = inject(ConversationPermissionHelper);

  readonly projectId = signal('');
  readonly agentFilterId = signal<string | null>(null);
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<ConversationStatus | ''>('ACTIVE', { nonNullable: true });
  readonly displayedColumns = ['title', 'status', 'messageCount', 'lastMessageAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<Conversation[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('lastMessageAt,desc');
  readonly statuses = CONVERSATION_STATUSES;

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    const agentId = this.route.snapshot.paramMap.get('agentId');
    this.agentFilterId.set(agentId);

    this.searchControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.statusControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.load();
  }

  load(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.conversationsApi
      .list(this.projectId(), {
        agentId: this.agentFilterId() ?? undefined,
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .subscribe({
        next: (page) => {
          this.rows.set(page.content);
          this.total.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err: { status?: number }) => {
          this.loading.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set('Unable to load conversations.');
        },
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  onSort(sort: Sort): void {
    this.sort.set(
      !sort.active || !sort.direction ? 'lastMessageAt,desc' : `${sort.active},${sort.direction}`,
    );
    this.load();
  }

  create(): void {
    void this.router.navigate(['/projects', this.projectId(), 'conversations', 'new'], {
      queryParams: this.agentFilterId() ? { agentId: this.agentFilterId() } : undefined,
    });
  }

  open(conversation: Conversation): void {
    void this.router.navigate([
      '/projects',
      this.projectId(),
      'conversations',
      conversation.id,
    ]);
  }

  archive(conversation: Conversation): void {
    if (!this.permissions.canArchive() || !window.confirm(`Archive conversation "${conversation.title}"?`)) {
      return;
    }
    this.conversationsApi.archive(this.projectId(), conversation.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to archive conversation.'),
    });
  }

  restore(conversation: Conversation): void {
    if (!this.permissions.canArchive() || !window.confirm(`Restore conversation "${conversation.title}"?`)) {
      return;
    }
    this.conversationsApi.restore(this.projectId(), conversation.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to restore conversation.'),
    });
  }

  statusClass(status: ConversationStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
