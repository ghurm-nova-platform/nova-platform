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

import { ToolDefinition, ToolStatus, ToolType, TOOL_STATUSES, TOOL_TYPES } from './tool.models';
import { ToolPermissionHelper } from './tool-permission.helper';
import { ToolService } from './tool.service';

@Component({
  selector: 'app-tools-list-page',
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
  templateUrl: './tools-list-page.html',
  styleUrl: './tools-list-page.scss',
})
export class ToolsListPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toolsApi = inject(ToolService);
  readonly permissions = inject(ToolPermissionHelper);

  readonly projectId = signal('');
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<ToolStatus | ''>('', { nonNullable: true });
  readonly typeControl = new FormControl<ToolType | ''>('', { nonNullable: true });
  readonly displayedColumns = ['name', 'toolKey', 'status', 'type', 'executorKey', 'updatedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<ToolDefinition[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = TOOL_STATUSES;
  readonly types = TOOL_TYPES;

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
    this.typeControl.valueChanges.subscribe(() => {
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
    this.toolsApi
      .listTools(this.projectId(), {
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        type: this.typeControl.value || undefined,
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
          this.error.set('Unable to load tools.');
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
    void this.router.navigate(['/projects', this.projectId(), 'tools', 'new']);
  }

  open(tool: ToolDefinition): void {
    void this.router.navigate(['/projects', this.projectId(), 'tools', tool.id]);
  }

  activate(tool: ToolDefinition): void {
    if (!this.permissions.canActivate() || !window.confirm(`Activate tool "${tool.name}"?`)) {
      return;
    }
    this.toolsApi.activateTool(this.projectId(), tool.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to activate tool.'),
    });
  }

  archive(tool: ToolDefinition): void {
    if (!this.permissions.canArchive() || !window.confirm(`Archive tool "${tool.name}"?`)) {
      return;
    }
    this.toolsApi.archiveTool(this.projectId(), tool.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to archive tool.'),
    });
  }

  statusClass(status: ToolStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
