import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  AgentKnowledgeAssignRequest,
  AgentKnowledgeAssignment,
  AgentKnowledgeAssignmentUpdateRequest,
  EmbeddingProvidersResponse,
  KnowledgeBase,
  KnowledgeBaseCreateRequest,
  KnowledgeBaseListParams,
  KnowledgeBaseUpdateRequest,
  KnowledgeChunk,
  KnowledgeChunkListParams,
  KnowledgeDocument,
  KnowledgeDocumentListParams,
} from './knowledge.models';

@Injectable({ providedIn: 'root' })
export class KnowledgeService {
  private readonly api = inject(ApiClient);

  createKnowledgeBase(projectId: string, body: KnowledgeBaseCreateRequest): Observable<KnowledgeBase> {
    return this.api.post<KnowledgeBase>(`/api/projects/${projectId}/knowledge-bases`, body);
  }

  listKnowledgeBases(
    projectId: string,
    params: KnowledgeBaseListParams = {},
  ): Observable<PageResponse<KnowledgeBase>> {
    return this.api.get<PageResponse<KnowledgeBase>>(
      `/api/projects/${projectId}/knowledge-bases`,
      this.toKnowledgeBaseQuery(params),
    );
  }

  getKnowledgeBase(projectId: string, knowledgeBaseId: string): Observable<KnowledgeBase> {
    return this.api.get<KnowledgeBase>(`/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}`);
  }

  updateKnowledgeBase(
    projectId: string,
    knowledgeBaseId: string,
    body: KnowledgeBaseUpdateRequest,
  ): Observable<KnowledgeBase> {
    return this.api.put<KnowledgeBase>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}`,
      body,
    );
  }

  activateKnowledgeBase(projectId: string, knowledgeBaseId: string): Observable<KnowledgeBase> {
    return this.api.post<KnowledgeBase>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/activate`,
      {},
    );
  }

  archiveKnowledgeBase(projectId: string, knowledgeBaseId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}`);
  }

  listProviders(projectId: string): Observable<EmbeddingProvidersResponse> {
    return this.api.get<EmbeddingProvidersResponse>(
      `/api/projects/${projectId}/knowledge-bases/providers`,
    );
  }

  uploadDocument(
    projectId: string,
    knowledgeBaseId: string,
    formData: FormData,
  ): Observable<KnowledgeDocument> {
    return this.api.postFormData<KnowledgeDocument>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents`,
      formData,
    );
  }

  listDocuments(
    projectId: string,
    knowledgeBaseId: string,
    params: KnowledgeDocumentListParams = {},
  ): Observable<PageResponse<KnowledgeDocument>> {
    return this.api.get<PageResponse<KnowledgeDocument>>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents`,
      this.toDocumentQuery(params),
    );
  }

  getDocument(
    projectId: string,
    knowledgeBaseId: string,
    documentId: string,
  ): Observable<KnowledgeDocument> {
    return this.api.get<KnowledgeDocument>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`,
    );
  }

  listDocumentChunks(
    projectId: string,
    knowledgeBaseId: string,
    documentId: string,
    params: KnowledgeChunkListParams = {},
  ): Observable<PageResponse<KnowledgeChunk>> {
    return this.api.get<PageResponse<KnowledgeChunk>>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/chunks`,
      this.toChunkQuery(params),
    );
  }

  reprocessDocument(
    projectId: string,
    knowledgeBaseId: string,
    documentId: string,
  ): Observable<KnowledgeDocument> {
    return this.api.post<KnowledgeDocument>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/reprocess`,
      {},
    );
  }

  archiveDocument(
    projectId: string,
    knowledgeBaseId: string,
    documentId: string,
  ): Observable<void> {
    return this.api.delete<void>(
      `/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`,
    );
  }

  listAgentKnowledgeBases(projectId: string, agentId: string): Observable<AgentKnowledgeAssignment[]> {
    return this.api.get<AgentKnowledgeAssignment[]>(
      `/api/projects/${projectId}/agents/${agentId}/knowledge-bases`,
    );
  }

  assignKnowledgeBase(
    projectId: string,
    agentId: string,
    body: AgentKnowledgeAssignRequest,
  ): Observable<AgentKnowledgeAssignment> {
    return this.api.post<AgentKnowledgeAssignment>(
      `/api/projects/${projectId}/agents/${agentId}/knowledge-bases`,
      body,
    );
  }

  updateKnowledgeAssignment(
    projectId: string,
    agentId: string,
    knowledgeBaseId: string,
    body: AgentKnowledgeAssignmentUpdateRequest,
  ): Observable<AgentKnowledgeAssignment> {
    return this.api.put<AgentKnowledgeAssignment>(
      `/api/projects/${projectId}/agents/${agentId}/knowledge-bases/${knowledgeBaseId}`,
      body,
    );
  }

  unassignKnowledgeBase(projectId: string, agentId: string, knowledgeBaseId: string): Observable<void> {
    return this.api.delete<void>(
      `/api/projects/${projectId}/agents/${agentId}/knowledge-bases/${knowledgeBaseId}`,
    );
  }

  private toKnowledgeBaseQuery(params: KnowledgeBaseListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.page !== undefined) {
      query['page'] = String(params.page);
    }
    if (params.size !== undefined) {
      query['size'] = String(params.size);
    }
    if (params.sort) {
      query['sort'] = params.sort;
    }
    return query;
  }

  private toDocumentQuery(params: KnowledgeDocumentListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.page !== undefined) {
      query['page'] = String(params.page);
    }
    if (params.size !== undefined) {
      query['size'] = String(params.size);
    }
    if (params.sort) {
      query['sort'] = params.sort;
    }
    return query;
  }

  private toChunkQuery(params: KnowledgeChunkListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.page !== undefined) {
      query['page'] = String(params.page);
    }
    if (params.size !== undefined) {
      query['size'] = String(params.size);
    }
    if (params.sort) {
      query['sort'] = params.sort;
    }
    return query;
  }
}
