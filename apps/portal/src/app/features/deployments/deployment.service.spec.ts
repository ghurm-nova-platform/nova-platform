import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { DeploymentService } from './deployment.service';

describe('DeploymentService', () => {
  it('posts observe and loads environments', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'RUNNING' }));
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [DeploymentService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(DeploymentService);
    service.observe({ releaseId: 'r1', environment: 'QA', deploymentProvider: 'LOCAL' }).subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/deployments/observe', {
      releaseId: 'r1',
      environment: 'QA',
      deploymentProvider: 'LOCAL',
    });
    service.environments().subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/deployments/environments');
    service.history('d1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/deployments/d1/history');
  });
});
