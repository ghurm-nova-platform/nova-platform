import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { Project } from '../../core/models/catalog';
import { ProjectService } from '../projects/project.service';
import { KnowledgeEnginePermissionHelper } from './knowledge-engine-permission.helper';
import { KnowledgeEngineService } from './knowledge-engine.service';
import {
  Category,
  DocumentDetail,
  DocumentSummary,
  KNOWLEDGE_CATEGORIES,
  KNOWLEDGE_TYPES,
  KnowledgeType,
  SearchResult,
  VISIBILITY_OPTIONS,
  Visibility,
} from './knowledge-engine.models';
import { paginateItems, renderMarkdownPreview } from './knowledge-viewer.helper';

@Component({
  selector: 'app-knowledge-engine-page',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTabsModule,
  ],
  templateUrl: './knowledge-engine-page.html',
  styleUrl: './knowledge-engine-page.scss',
})
export class KnowledgeEnginePage implements OnInit {
  private readonly knowledgeApi = inject(KnowledgeEngineService);
  private readonly projectApi = inject(ProjectService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly destroyRef = inject(DestroyRef);
  readonly permissions = inject(KnowledgeEnginePermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);

  readonly documents = signal<DocumentSummary[]>([]);
  readonly selectedDocumentId = signal<string | null>(null);
  readonly documentDetail = signal<DocumentDetail | null>(null);

  readonly categories = signal<string[]>([]);
  readonly tags = signal<string[]>([]);
  readonly projects = signal<Project[]>([]);
  readonly selectedProjectId = signal<string | null>(null);
  readonly projectDocuments = signal<DocumentSummary[]>([]);

  readonly searchQuery = signal('');
  readonly searchCategory = signal<Category | ''>('');
  readonly searchType = signal<KnowledgeType | ''>('');
  readonly searchTag = signal('');
  readonly searchVisibility = signal<Visibility | ''>('');
  readonly searchResults = signal<SearchResult[]>([]);
  readonly searchLoading = signal(false);
  readonly searchPageIndex = signal(0);
  readonly searchPageSize = signal(10);

  readonly importTitle = signal('');
  readonly importContent = signal('');
  readonly importCategory = signal<Category>('General');
  readonly importType = signal<KnowledgeType>('DOCUMENTATION');
  readonly importVisibility = signal<Visibility>('ORGANIZATION');
  readonly importTags = signal('');
  readonly importSuccess = signal<string | null>(null);

  readonly categoryOptions = KNOWLEDGE_CATEGORIES;
  readonly typeOptions = KNOWLEDGE_TYPES;
  readonly visibilityOptions = VISIBILITY_OPTIONS;

  constructor() {
    toObservable(this.searchQuery)
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(() => {
          if (!this.permissions.canRead()) {
            return of([]);
          }
          this.searchLoading.set(true);
          return this.knowledgeApi.search(this.buildSearchParams());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (results) => {
          this.searchResults.set(results);
          this.searchPageIndex.set(0);
          this.searchLoading.set(false);
        },
        error: (err) => {
          this.searchLoading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to search knowledge documents');
        },
      });
  }

  readonly selectedDocument = computed(() => {
    const id = this.selectedDocumentId();
    if (!id) {
      return null;
    }
    return this.documents().find((document) => document.id === id) ?? null;
  });

  readonly paginatedSearchResults = computed(() =>
    paginateItems(this.searchResults(), this.searchPageIndex(), this.searchPageSize()),
  );

  readonly searchTotal = computed(() => this.searchResults().length);

  readonly viewerHtml = computed((): SafeHtml | null => {
    const detail = this.documentDetail();
    if (!detail?.content) {
      return null;
    }
    return this.sanitizer.bypassSecurityTrustHtml(renderMarkdownPreview(detail.content));
  });

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      return;
    }
    this.loadDocuments();
    this.loadCategories();
    this.loadTags();
    this.loadProjects();
  }

  selectDocument(document: DocumentSummary): void {
    this.selectedDocumentId.set(document.id);
    this.documentDetail.set(null);
    this.loadDocumentDetail(document.id);
  }

  refreshDocuments(): void {
    this.loadDocuments();
  }

  refreshSelected(): void {
    const id = this.selectedDocumentId();
    if (!id) {
      return;
    }
    this.loadDocumentDetail(id);
  }

  onSearchFiltersChanged(): void {
    this.runSearch();
  }

  onSearchPage(event: PageEvent): void {
    this.searchPageIndex.set(event.pageIndex);
    this.searchPageSize.set(event.pageSize);
  }

  selectProject(projectId: string): void {
    this.selectedProjectId.set(projectId);
    this.loading.set(true);
    this.knowledgeApi.listByProject(projectId).subscribe({
      next: (items) => {
        this.projectDocuments.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load project documents');
      },
    });
  }

  importDocument(): void {
    if (!this.permissions.canWrite()) {
      return;
    }
    const title = this.importTitle().trim();
    const content = this.importContent().trim();
    if (!title || !content) {
      this.error.set('Title and content are required for import');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.importSuccess.set(null);
    this.knowledgeApi
      .importDocument({
        title,
        content,
        contentFormat: 'MARKDOWN',
        knowledgeType: this.importType(),
        category: this.importCategory(),
        visibility: this.importVisibility(),
        tags: this.parseTags(this.importTags()),
        importFormat: 'markdown',
      })
      .subscribe({
        next: (detail) => {
          this.loading.set(false);
          this.importSuccess.set(`Imported "${detail.title}"`);
          this.importTitle.set('');
          this.importContent.set('');
          this.loadDocuments();
          this.selectDocument(detail);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to import document');
        },
      });
  }

  statusClass(status: string): string {
    return `knowledge-engine__status knowledge-engine__status--${status.toLowerCase()}`;
  }

  private loadDocuments(): void {
    this.loading.set(true);
    this.error.set(null);
    this.knowledgeApi.list().subscribe({
      next: (items) => {
        this.documents.set(items);
        if (items.length > 0 && !this.selectedDocumentId()) {
          this.selectDocument(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load knowledge documents');
      },
    });
  }

  private loadDocumentDetail(id: string): void {
    this.loading.set(true);
    this.knowledgeApi.get(id).subscribe({
      next: (detail) => {
        this.documentDetail.set(detail);
        this.loading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load document detail');
      },
    });
  }

  private loadCategories(): void {
    this.knowledgeApi.categories().subscribe({
      next: (items) => this.categories.set(items),
      error: () => this.categories.set([]),
    });
  }

  private loadTags(): void {
    this.knowledgeApi.tags().subscribe({
      next: (items) => this.tags.set(items),
      error: () => this.tags.set([]),
    });
  }

  private loadProjects(): void {
    this.projectApi.list({ size: 100 }).subscribe({
      next: (page) => this.projects.set(page.content),
      error: () => this.projects.set([]),
    });
  }

  private runSearch(): void {
    if (!this.permissions.canRead()) {
      return;
    }
    this.searchLoading.set(true);
    this.knowledgeApi.search(this.buildSearchParams()).subscribe({
      next: (results) => {
        this.searchResults.set(results);
        this.searchPageIndex.set(0);
        this.searchLoading.set(false);
      },
      error: (err) => {
        this.searchLoading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to search knowledge documents');
      },
    });
  }

  private buildSearchParams() {
    return {
      q: this.searchQuery().trim() || undefined,
      category: this.searchCategory() || undefined,
      knowledgeType: this.searchType() || undefined,
      tag: this.searchTag().trim() || undefined,
      visibility: this.searchVisibility() || undefined,
    };
  }

  private parseTags(value: string): string[] {
    return value
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean);
  }
}

export { renderMarkdownPreview, paginateItems };
