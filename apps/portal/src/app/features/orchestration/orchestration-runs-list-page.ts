import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Project } from '../../core/models/catalog';
import { ProjectService } from '../projects/project.service';
import { OrchestrationPermissionHelper } from './orchestration-permission.helper';
import { OrchestrationRunService } from './orchestration-run.service';
import {
  EXECUTION_MODES,
  ExecutionMode,
  OrchestrationRun,
  RUN_STATUSES,
  RunStatus,
} from './orchestration.models';

@Component({
  selector: 'app-orchestration-runs-list-page',
  imports: [
    DatePipe,
    DecimalPipe,
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
  templateUrl: './orchestration-runs-list-page.html',
  styleUrl: './orchestration-page.scss',
})
export class OrchestrationRunsListPage implements OnInit {
  private readonly router = inject(Router);
  private readonly runsApi = inject(OrchestrationRunService);
  private readonly projectsApi = inject(ProjectService);
  readonly permissions = inject(OrchestrationPermissionHelper);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<RunStatus | ''>('', { nonNullable: true });
  readonly modeControl = new FormControl<ExecutionMode | ''>('', { nonNullable: true });
  readonly projectControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = [
    'name',
    'status',
    'executionMode',
    'completedPercentage',
    'runningTaskCount',
    'updatedAt',
    'actions',
  ];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<OrchestrationRun[]>([]);
  readonly projects = signal<Project[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = RUN_STATUSES;
  readonly modes = EXECUTION_MODES;

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.statusControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.modeControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.projectControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.loadProjects();
    this.load();
  }

  load(): void {
    if (!this.permissions.canReadRuns()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.runsApi
      .list({
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        executionMode: this.modeControl.value || undefined,
        projectId: this.projectControl.value || undefined,
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
          this.error.set('Unable to load orchestration runs.');
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
    void this.router.navigate(['/orchestration-runs', 'new']);
  }

  open(run: OrchestrationRun): void {
    void this.router.navigate(['/orchestration-runs', run.id]);
  }

  edit(run: OrchestrationRun): void {
    void this.router.navigate(['/orchestration-runs', run.id, 'edit']);
  }

  statusClass(status: RunStatus): string {
    return `status status--${status.toLowerCase().replace(/_/g, '-')}`;
  }

  private loadProjects(): void {
    this.projectsApi.list({ page: 0, size: 100, sort: 'name,asc' }).subscribe({
      next: (page) => this.projects.set(page.content),
      error: () => this.projects.set([]),
    });
  }
}
