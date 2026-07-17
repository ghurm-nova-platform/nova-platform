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
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Prompt, PromptStatus, PromptType, PROMPT_STATUSES, PROMPT_TYPES } from './prompt.models';
import { PromptPermissionHelper } from './prompt-permission.helper';
import { PromptService } from './prompt.service';

@Component({
  selector: 'app-prompts-list-page',
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
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './prompts-list-page.html',
  styleUrl: './prompts-list-page.scss',
})
export class PromptsListPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly promptsApi = inject(PromptService);
  readonly permissions = inject(PromptPermissionHelper);

  readonly projectId = signal('');
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<PromptStatus | ''>('', { nonNullable: true });
  readonly typeControl = new FormControl<PromptType | ''>('', { nonNullable: true });
  readonly tagControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = ['name', 'status', 'type', 'tags', 'updatedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<Prompt[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = PROMPT_STATUSES;
  readonly types = PROMPT_TYPES;

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
    this.tagControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
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
    this.promptsApi
      .list(this.projectId(), {
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        type: this.typeControl.value || undefined,
        tag: this.tagControl.value.trim() || undefined,
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
          this.error.set('Unable to load prompts.');
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
    void this.router.navigate(['/projects', this.projectId(), 'prompts', 'new']);
  }

  open(prompt: Prompt): void {
    void this.router.navigate(['/projects', this.projectId(), 'prompts', prompt.id]);
  }

  archive(prompt: Prompt): void {
    if (!window.confirm(`Archive prompt "${prompt.name}"?`)) {
      return;
    }
    this.promptsApi.archive(this.projectId(), prompt.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to archive prompt.'),
    });
  }

  statusClass(status: PromptStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
