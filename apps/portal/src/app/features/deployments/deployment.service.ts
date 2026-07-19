import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { Deployment, DeploymentEnvironment, ObserveDeploymentRequest } from './deployment.models';

@Injectable({ providedIn: 'root' })
export class DeploymentService {
  private readonly api = inject(ApiClient);

  observe(request: ObserveDeploymentRequest): Observable<Deployment> {
    return this.api.post<Deployment>('/api/deployments/observe', request);
  }

  verify(id: string): Observable<Deployment> {
    return this.api.post<Deployment>(`/api/deployments/${id}/verify`, {});
  }

  list(projectId?: string): Observable<Deployment[]> {
    const query = projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
    return this.api.get<Deployment[]>(`/api/deployments${query}`);
  }

  get(id: string): Observable<Deployment> {
    return this.api.get<Deployment>(`/api/deployments/${id}`);
  }

  history(id: string): Observable<Deployment> {
    return this.api.get<Deployment>(`/api/deployments/${id}/history`);
  }

  environments(): Observable<DeploymentEnvironment[]> {
    return this.api.get<DeploymentEnvironment[]>('/api/deployments/environments');
  }
}
