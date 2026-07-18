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
  KnowledgeBase,
  KnowledgeDocument,
  KnowledgeDocumentStatus,
  KNOWLEDGE_DOCUMENT_STATUSES,
} from './knowledge.models';
import { KnowledgePermissionHelper } from './knowledge-permission.helper';
import { KnowledgeService } from './knowledge.service';

@Component({
  selector: 'app-knowledge-documents-page',
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
  templateUrl: './knowledge-documents-page.html',
  styleUrl: './knowledge-documents-page.scss',
})
export class KnowledgeDocumentsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly knowledgeApi = inject(KnowledgeService);
  readonly permissions = inject(KnowledgePermissionHelper);

  readonly projectId = signal('');
  readonly knowledgeBaseId = signal('');
  readonly knowledgeBase = signal<KnowledgeBase | null>(null);
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<KnowledgeDocumentStatus | ''>('', { nonNullable: true });
  readonly documentKeyControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = [
    'fileName',
    'documentKey',
    'documentType',
    'status',
    'chunkCount',
    'updatedAt',
    'actions',
  ];
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<KnowledgeDocument[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = KNOWLEDGE_DOCUMENT_STATUSES;
  readonly pdfEnabled = signal(false);

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.knowledgeBaseId.set(this.route.snapshot.paramMap.get('knowledgeBaseId') ?? '');
    this.searchControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.loadDocuments();
    });
    this.statusControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.loadDocuments();
    });
    this.loadKnowledgeBase();
    this.loadDocuments();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.permissions.canUploadDocuments() || this.uploading()) {
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    const documentKey = this.documentKeyControl.value.trim();
    if (documentKey) {
      formData.append('documentKey', documentKey);
    }

    this.uploading.set(true);
    this.error.set(null);
    this.knowledgeApi
      .uploadDocument(this.projectId(), this.knowledgeBaseId(), formData)
      .subscribe({
        next: () => {
          this.uploading.set(false);
          this.documentKeyControl.reset();
          input.value = '';
          this.loadDocuments();
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.uploading.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to upload document.');
        },
      });
  }

  loadDocuments(): void {
    if (!this.permissions.canReadDocuments()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.knowledgeApi
      .listDocuments(this.projectId(), this.knowledgeBaseId(), {
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
          this.error.set('Unable to load documents.');
        },
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadDocuments();
  }

  onSort(sort: Sort): void {
    this.sort.set(!sort.active || !sort.direction ? 'createdAt,desc' : `${sort.active},${sort.direction}`);
    this.loadDocuments();
  }

  open(document: KnowledgeDocument): void {
    void this.router.navigate([
      '/projects',
      this.projectId(),
      'knowledge-bases',
      this.knowledgeBaseId(),
      'documents',
      document.id,
    ]);
  }

  reprocess(document: KnowledgeDocument): void {
    if (
      !this.permissions.canReprocessDocuments() ||
      !window.confirm(`Reprocess document "${document.fileName}"?`)
    ) {
      return;
    }
    this.knowledgeApi
      .reprocessDocument(this.projectId(), this.knowledgeBaseId(), document.id)
      .subscribe({
        next: () => this.loadDocuments(),
        error: () => this.error.set('Unable to reprocess document.'),
      });
  }

  archive(document: KnowledgeDocument): void {
    if (
      !this.permissions.canArchiveDocuments() ||
      !window.confirm(`Archive document "${document.fileName}"?`)
    ) {
      return;
    }
    this.knowledgeApi
      .archiveDocument(this.projectId(), this.knowledgeBaseId(), document.id)
      .subscribe({
        next: () => this.loadDocuments(),
        error: () => this.error.set('Unable to archive document.'),
      });
  }

  statusClass(status: KnowledgeDocumentStatus): string {
    return `status status--${status.toLowerCase().replace(/_/g, '-')}`;
  }

  private loadKnowledgeBase(): void {
    if (!this.permissions.canRead()) {
      return;
    }
    this.knowledgeApi.getKnowledgeBase(this.projectId(), this.knowledgeBaseId()).subscribe({
      next: (knowledgeBase) => this.knowledgeBase.set(knowledgeBase),
      error: () => this.error.set('Unable to load knowledge base.'),
    });
  }
}
