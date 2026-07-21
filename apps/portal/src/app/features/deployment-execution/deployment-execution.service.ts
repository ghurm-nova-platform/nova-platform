import { Injectable, inject } from '@angular/core';

import { ApiClient } from '../../core/http/api-client';
import {
  CreateExecutionRequest,
  DeploymentExecution,
  ExecutionLogEntry,
} from './deployment-execution.models';

@Injectable({ providedIn: 'root' })
export class DeploymentExecutionService {
  private readonly api = inject(ApiClient);

  create(request: CreateExecutionRequest) {
    return this.api.post<DeploymentExecution>('/api/deployment-executions/create', request);
  }

  start(id: string) {
    return this.api.post<DeploymentExecution>(`/api/deployment-executions/${id}/start`, {});
  }

  cancel(id: string) {
    return this.api.post<DeploymentExecution>(`/api/deployment-executions/${id}/cancel`, {});
  }

  list(projectId?: string) {
    const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
    return this.api.get<DeploymentExecution[]>(`/api/deployment-executions${query}`);
  }

  get(id: string) {
    return this.api.get<DeploymentExecution>(`/api/deployment-executions/${id}`);
  }

  history(id: string) {
    return this.api.get<DeploymentExecution>(`/api/deployment-executions/${id}/history`);
  }

  logs(id: string) {
    return this.api.get<ExecutionLogEntry[]>(`/api/deployment-executions/${id}/logs`);
  }
}
