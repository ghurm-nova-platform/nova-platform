import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PageResponse } from '../../core/models/catalog';
import {
  CancelOrchestrationRunRequest,
  CreateOrchestrationDependencyRequest,
  CreateOrchestrationRunRequest,
  CreateOrchestrationTaskRequest,
  DeleteOrchestrationDependencyRequest,
  OrchestrationAttempt,
  OrchestrationDependency,
  OrchestrationEvent,
  OrchestrationEventListParams,
  OrchestrationGraph,
  OrchestrationRun,
  OrchestrationRunListParams,
  OrchestrationTask,
  OrchestrationTaskListParams,
  UpdateOrchestrationRunRequest,
  UpdateOrchestrationTaskRequest,
} from './orchestration.models';

@Injectable({ providedIn: 'root' })
export class OrchestrationRunService {
  private readonly api = inject(ApiClient);

  list(params: OrchestrationRunListParams = {}): Observable<PageResponse<OrchestrationRun>> {
    return this.api.get<PageResponse<OrchestrationRun>>('/api/orchestration-runs', this.toRunQuery(params));
  }

  get(runId: string): Observable<OrchestrationRun> {
    return this.api.get<OrchestrationRun>(`/api/orchestration-runs/${runId}`);
  }

  create(body: CreateOrchestrationRunRequest): Observable<OrchestrationRun> {
    return this.api.post<OrchestrationRun>('/api/orchestration-runs', body);
  }

  update(runId: string, body: UpdateOrchestrationRunRequest): Observable<OrchestrationRun> {
    return this.api.put<OrchestrationRun>(`/api/orchestration-runs/${runId}`, body);
  }

  ready(runId: string): Observable<OrchestrationRun> {
    return this.api.post<OrchestrationRun>(`/api/orchestration-runs/${runId}/ready`, {});
  }

  start(runId: string): Observable<OrchestrationRun> {
    return this.api.post<OrchestrationRun>(`/api/orchestration-runs/${runId}/start`, {});
  }

  cancel(runId: string, body: CancelOrchestrationRunRequest = {}): Observable<OrchestrationRun> {
    return this.api.post<OrchestrationRun>(`/api/orchestration-runs/${runId}/cancel`, body);
  }

  archive(runId: string): Observable<OrchestrationRun> {
    return this.api.post<OrchestrationRun>(`/api/orchestration-runs/${runId}/archive`, {});
  }

  listTasks(
    runId: string,
    params: OrchestrationTaskListParams = {},
  ): Observable<PageResponse<OrchestrationTask>> {
    return this.api.get<PageResponse<OrchestrationTask>>(
      `/api/orchestration-runs/${runId}/tasks`,
      this.toTaskQuery(params),
    );
  }

  getTask(runId: string, taskId: string): Observable<OrchestrationTask> {
    return this.api.get<OrchestrationTask>(`/api/orchestration-runs/${runId}/tasks/${taskId}`);
  }

  createTask(runId: string, body: CreateOrchestrationTaskRequest): Observable<OrchestrationTask> {
    return this.api.post<OrchestrationTask>(`/api/orchestration-runs/${runId}/tasks`, body);
  }

  updateTask(
    runId: string,
    taskId: string,
    body: UpdateOrchestrationTaskRequest,
  ): Observable<OrchestrationTask> {
    return this.api.put<OrchestrationTask>(`/api/orchestration-runs/${runId}/tasks/${taskId}`, body);
  }

  deleteTask(runId: string, taskId: string): Observable<void> {
    return this.api.delete<void>(`/api/orchestration-runs/${runId}/tasks/${taskId}`);
  }

  listAttempts(runId: string, taskId: string): Observable<OrchestrationAttempt[]> {
    return this.api.get<OrchestrationAttempt[]>(
      `/api/orchestration-runs/${runId}/tasks/${taskId}/attempts`,
    );
  }

  addDependency(
    runId: string,
    body: CreateOrchestrationDependencyRequest,
  ): Observable<OrchestrationDependency> {
    return this.api.post<OrchestrationDependency>(`/api/orchestration-runs/${runId}/dependencies`, body);
  }

  removeDependency(runId: string, body: DeleteOrchestrationDependencyRequest): Observable<void> {
    return this.api.delete<void>(`/api/orchestration-runs/${runId}/dependencies`, body);
  }

  getGraph(runId: string): Observable<OrchestrationGraph> {
    return this.api.get<OrchestrationGraph>(`/api/orchestration-runs/${runId}/graph`);
  }

  listEvents(
    runId: string,
    params: OrchestrationEventListParams = {},
  ): Observable<PageResponse<OrchestrationEvent>> {
    return this.api.get<PageResponse<OrchestrationEvent>>(
      `/api/orchestration-runs/${runId}/events`,
      this.toEventQuery(params),
    );
  }

  private toRunQuery(params: OrchestrationRunListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.projectId) {
      query['projectId'] = params.projectId;
    }
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.executionMode) {
      query['executionMode'] = params.executionMode;
    }
    if (params.createdBy) {
      query['createdBy'] = params.createdBy;
    }
    if (params.search) {
      query['search'] = params.search;
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

  private toTaskQuery(params: OrchestrationTaskListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.status) {
      query['status'] = params.status;
    }
    if (params.taskType) {
      query['taskType'] = params.taskType;
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

  private toEventQuery(params: OrchestrationEventListParams): Record<string, string> {
    const query: Record<string, string> = {};
    if (params.taskId) {
      query['taskId'] = params.taskId;
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
