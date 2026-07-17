import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  Prompt,
  PromptCompareRequest,
  PromptCompareResponse,
  PromptCreateRequest,
  PromptPreviewRequest,
  PromptPreviewResponse,
  PromptPublishRequest,
  PromptRollbackRequest,
  PromptStatus,
  PromptType,
  PromptUpdateRequest,
  PromptValidateRequest,
  PromptValidateResponse,
  PromptVersion,
  PromptVersionCreateRequest,
  PromptVersionUpdateRequest,
} from './prompt.models';

export interface PromptListParams {
  search?: string;
  status?: PromptStatus;
  type?: PromptType;
  tag?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class PromptService {
  private readonly api = inject(ApiClient);

  list(projectId: string, params: PromptListParams = {}): Observable<PageResponse<Prompt>> {
    return this.api.get<PageResponse<Prompt>>(
      `/api/projects/${projectId}/prompts`,
      this.toQuery(params),
    );
  }

  get(projectId: string, promptId: string): Observable<Prompt> {
    return this.api.get<Prompt>(`/api/projects/${projectId}/prompts/${promptId}`);
  }

  create(projectId: string, body: PromptCreateRequest): Observable<Prompt> {
    return this.api.post<Prompt>(`/api/projects/${projectId}/prompts`, body);
  }

  update(projectId: string, promptId: string, body: PromptUpdateRequest): Observable<Prompt> {
    return this.api.put<Prompt>(`/api/projects/${projectId}/prompts/${promptId}`, body);
  }

  archive(projectId: string, promptId: string): Observable<void> {
    return this.api.delete<void>(`/api/projects/${projectId}/prompts/${promptId}`);
  }

  listVersions(projectId: string, promptId: string): Observable<PromptVersion[]> {
    return this.api.get<PromptVersion[]>(`/api/projects/${projectId}/prompts/${promptId}/versions`);
  }

  getVersion(projectId: string, promptId: string, versionId: string): Observable<PromptVersion> {
    return this.api.get<PromptVersion>(
      `/api/projects/${projectId}/prompts/${promptId}/versions/${versionId}`,
    );
  }

  createVersion(
    projectId: string,
    promptId: string,
    body: PromptVersionCreateRequest = {},
  ): Observable<PromptVersion> {
    return this.api.post<PromptVersion>(
      `/api/projects/${projectId}/prompts/${promptId}/versions`,
      body,
    );
  }

  updateVersion(
    projectId: string,
    promptId: string,
    versionId: string,
    body: PromptVersionUpdateRequest,
  ): Observable<PromptVersion> {
    return this.api.put<PromptVersion>(
      `/api/projects/${projectId}/prompts/${promptId}/versions/${versionId}`,
      body,
    );
  }

  publishVersion(
    projectId: string,
    promptId: string,
    versionId: string,
    body: PromptPublishRequest = {},
  ): Observable<PromptVersion> {
    return this.api.post<PromptVersion>(
      `/api/projects/${projectId}/prompts/${promptId}/versions/${versionId}/publish`,
      body,
    );
  }

  rollback(
    projectId: string,
    promptId: string,
    body: PromptRollbackRequest,
  ): Observable<Prompt> {
    return this.api.post<Prompt>(`/api/projects/${projectId}/prompts/${promptId}/rollback`, body);
  }

  compare(
    projectId: string,
    promptId: string,
    body: PromptCompareRequest,
  ): Observable<PromptCompareResponse> {
    return this.api.post<PromptCompareResponse>(
      `/api/projects/${projectId}/prompts/${promptId}/compare`,
      body,
    );
  }

  validate(
    projectId: string,
    body: PromptValidateRequest,
  ): Observable<PromptValidateResponse> {
    return this.api.post<PromptValidateResponse>(
      `/api/projects/${projectId}/prompts/validate`,
      body,
    );
  }

  preview(projectId: string, body: PromptPreviewRequest): Observable<PromptPreviewResponse> {
    return this.api.post<PromptPreviewResponse>(
      `/api/projects/${projectId}/prompts/preview`,
      body,
    );
  }

  private toQuery(params: PromptListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.search) {
      query['search'] = params.search;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.type) {
      query['type'] = params.type;
    }
    if (params.tag) {
      query['tag'] = params.tag;
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
}
