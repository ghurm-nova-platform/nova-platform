import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import {
  CreateDocumentRequest,
  DocumentDetail,
  DocumentSummary,
  ImportDocumentRequest,
  KnowledgeEngineConfigResponse,
  KnowledgeMemoryParams,
  KnowledgeSearchParams,
  MemoryDocument,
  RelateDocumentRequest,
  RelationView,
  SearchResult,
  UpdateDocumentRequest,
} from './knowledge-engine.models';

@Injectable({ providedIn: 'root' })
export class KnowledgeEngineService {
  private readonly api = inject(ApiClient);
  private readonly http = inject(HttpClient);
  private readonly runtimeConfig = inject(RuntimeConfigService);

  config(): Observable<KnowledgeEngineConfigResponse> {
    return this.api.get<KnowledgeEngineConfigResponse>('/api/knowledge/config');
  }

  list(projectId?: string): Observable<DocumentSummary[]> {
    return this.api.get<DocumentSummary[]>(`/api/knowledge${this.projectQuery(projectId)}`);
  }

  search(params: KnowledgeSearchParams = {}): Observable<SearchResult[]> {
    return this.api.get<SearchResult[]>('/api/knowledge/search', this.searchQuery(params));
  }

  memory(params: KnowledgeMemoryParams = {}): Observable<MemoryDocument[]> {
    return this.api.get<MemoryDocument[]>('/api/knowledge/memory', this.memoryQuery(params));
  }

  categories(): Observable<string[]> {
    return this.api.get<string[]>('/api/knowledge/categories');
  }

  tags(): Observable<string[]> {
    return this.api.get<string[]>('/api/knowledge/tags');
  }

  listByProject(projectId: string): Observable<DocumentSummary[]> {
    return this.api.get<DocumentSummary[]>(`/api/knowledge/project/${encodeURIComponent(projectId)}`);
  }

  get(id: string): Observable<DocumentDetail> {
    return this.api.get<DocumentDetail>(`/api/knowledge/${encodeURIComponent(id)}`);
  }

  export(id: string, format = 'markdown'): Observable<Blob> {
    const base = this.runtimeConfig.platformApiUrl().replace(/\/$/, '');
    return this.http.get(`${base}/api/knowledge/${encodeURIComponent(id)}/export`, {
      params: { format },
      responseType: 'blob',
    });
  }

  relations(id: string): Observable<RelationView[]> {
    return this.api.get<RelationView[]>(`/api/knowledge/${encodeURIComponent(id)}/relations`);
  }

  create(request: CreateDocumentRequest): Observable<DocumentDetail> {
    return this.api.post<DocumentDetail>('/api/knowledge', request);
  }

  importDocument(request: ImportDocumentRequest): Observable<DocumentDetail> {
    return this.api.post<DocumentDetail>('/api/knowledge/import', request);
  }

  archive(id: string): Observable<DocumentDetail> {
    return this.api.post<DocumentDetail>(`/api/knowledge/${encodeURIComponent(id)}/archive`, {});
  }

  restore(id: string): Observable<DocumentDetail> {
    return this.api.post<DocumentDetail>(`/api/knowledge/${encodeURIComponent(id)}/restore`, {});
  }

  relate(id: string, request: RelateDocumentRequest): Observable<RelationView[]> {
    return this.api.post<RelationView[]>(`/api/knowledge/${encodeURIComponent(id)}/relate`, request);
  }

  update(id: string, request: UpdateDocumentRequest): Observable<DocumentDetail> {
    return this.api.put<DocumentDetail>(`/api/knowledge/${encodeURIComponent(id)}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`/api/knowledge/${encodeURIComponent(id)}`);
  }

  private projectQuery(projectId?: string): string {
    return projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
  }

  private searchQuery(params: KnowledgeSearchParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.q) {
      query['q'] = params.q;
    }
    if (params.tag) {
      query['tag'] = params.tag;
    }
    if (params.category) {
      query['category'] = params.category;
    }
    if (params.projectId) {
      query['projectId'] = params.projectId;
    }
    if (params.authorId) {
      query['authorId'] = params.authorId;
    }
    if (params.visibility) {
      query['visibility'] = params.visibility;
    }
    if (params.knowledgeType) {
      query['knowledgeType'] = params.knowledgeType;
    }
    if (params.from) {
      query['from'] = params.from;
    }
    if (params.to) {
      query['to'] = params.to;
    }
    return query;
  }

  private memoryQuery(params: KnowledgeMemoryParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.projectId) {
      query['projectId'] = params.projectId;
    }
    if (params.limit !== undefined) {
      query['limit'] = String(params.limit);
    }
    if (params.types?.length) {
      query['types'] = params.types.join(',');
    }
    return query;
  }
}
