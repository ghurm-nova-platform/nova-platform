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

import { Agent, AgentStatus } from './agent.models';
import { ExecutionPermissionHelper } from '../execution/execution-permission.helper';
import { AgentPermissionHelper } from './agent-permission.helper';
import { AgentService } from './agent.service';

@Component({
  selector: 'app-agents-list-page',
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
  templateUrl: './agents-list-page.html',
  styleUrl: './agents-list-page.scss',
})
export class AgentsListPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agentsApi = inject(AgentService);
  readonly permissions = inject(AgentPermissionHelper);
  readonly executionPermissions = inject(ExecutionPermissionHelper);

  readonly projectId = signal('');
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<AgentStatus | ''>('', { nonNullable: true });
  readonly displayedColumns = ['name', 'status', 'model', 'updatedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<Agent[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses: AgentStatus[] = ['DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED'];

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
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
    this.agentsApi
      .list(this.projectId(), {
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
          this.error.set('Unable to load agents.');
        },
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  onSort(sort: Sort): void {
    this.sort.set(!sort.active || !sort.direction ? 'createdAt,desc' : `${sort.active},${sort.direction}`);
    this.load();
  }

  create(): void {
    void this.router.navigate(['/projects', this.projectId(), 'agents', 'new']);
  }

  open(agent: Agent): void {
    void this.router.navigate(['/projects', this.projectId(), 'agents', agent.id]);
  }

  playground(agent: Agent): void {
    void this.router.navigate(['/projects', this.projectId(), 'agents', agent.id, 'playground']);
  }

  archive(agent: Agent): void {
    if (!window.confirm(`Archive agent "${agent.name}"?`)) {
      return;
    }
    this.agentsApi.archive(this.projectId(), agent.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to archive agent.'),
    });
  }

  statusClass(status: AgentStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
