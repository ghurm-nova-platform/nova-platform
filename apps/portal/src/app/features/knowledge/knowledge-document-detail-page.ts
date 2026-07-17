import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { KnowledgeChunk, KnowledgeDocument, KnowledgeDocumentStatus } from './knowledge.models';
import { KnowledgePermissionHelper } from './knowledge-permission.helper';
import { KnowledgeService } from './knowledge.service';

@Component({
  selector: 'app-knowledge-document-detail-page',
  imports: [
    DatePipe,
    RouterLink,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './knowledge-document-detail-page.html',
  styleUrl: './knowledge-document-detail-page.scss',
})
export class KnowledgeDocumentDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly knowledgeApi = inject(KnowledgeService);
  readonly permissions = inject(KnowledgePermissionHelper);

  readonly projectId = signal('');
  readonly knowledgeBaseId = signal('');
  readonly documentId = signal('');
  readonly loading = signal(false);
  readonly chunksLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly document = signal<KnowledgeDocument | null>(null);
  readonly chunks = signal<KnowledgeChunk[]>([]);
  readonly chunksTotal = signal(0);
  readonly chunksPageIndex = signal(0);
  readonly chunksPageSize = signal(10);

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.knowledgeBaseId.set(this.route.snapshot.paramMap.get('knowledgeBaseId') ?? '');
    this.documentId.set(this.route.snapshot.paramMap.get('documentId') ?? '');
    this.loadDocument();
    this.loadChunks();
  }

  onChunksPage(event: PageEvent): void {
    this.chunksPageIndex.set(event.pageIndex);
    this.chunksPageSize.set(event.pageSize);
    this.loadChunks();
  }

  reprocess(): void {
    const current = this.document();
    if (
      !current ||
      !this.permissions.canReprocessDocuments() ||
      !window.confirm(`Reprocess document "${current.fileName}"?`)
    ) {
      return;
    }
    this.knowledgeApi
      .reprocessDocument(this.projectId(), this.knowledgeBaseId(), current.id)
      .subscribe({
        next: (updated) => {
          this.document.set(updated);
          this.loadChunks();
        },
        error: () => this.error.set('Unable to reprocess document.'),
      });
  }

  archive(): void {
    const current = this.document();
    if (
      !current ||
      !this.permissions.canArchiveDocuments() ||
      !window.confirm(`Archive document "${current.fileName}"?`)
    ) {
      return;
    }
    this.knowledgeApi
      .archiveDocument(this.projectId(), this.knowledgeBaseId(), current.id)
      .subscribe({
        next: () => {
          this.document.update((doc) => (doc ? { ...doc, status: 'ARCHIVED' } : doc));
        },
        error: () => this.error.set('Unable to archive document.'),
      });
  }

  statusClass(status: KnowledgeDocumentStatus): string {
    return `status status--${status.toLowerCase().replace(/_/g, '-')}`;
  }

  private loadDocument(): void {
    if (!this.permissions.canReadDocuments()) {
      this.unauthorized.set(true);
      return;
    }
    this.loading.set(true);
    this.knowledgeApi
      .getDocument(this.projectId(), this.knowledgeBaseId(), this.documentId())
      .subscribe({
        next: (document) => {
          this.document.set(document);
          this.loading.set(false);
        },
        error: (err: { status?: number }) => {
          this.loading.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set('Unable to load document.');
        },
      });
  }

  private loadChunks(): void {
    if (!this.permissions.canReadDocuments()) {
      this.chunks.set([]);
      return;
    }
    this.chunksLoading.set(true);
    this.knowledgeApi
      .listDocumentChunks(this.projectId(), this.knowledgeBaseId(), this.documentId(), {
        page: this.chunksPageIndex(),
        size: this.chunksPageSize(),
        sort: 'chunkIndex,asc',
      })
      .subscribe({
        next: (page) => {
          this.chunks.set(page.content);
          this.chunksTotal.set(page.totalElements);
          this.chunksLoading.set(false);
        },
        error: () => {
          this.chunksLoading.set(false);
          this.error.set('Unable to load document chunks.');
        },
      });
  }
}
