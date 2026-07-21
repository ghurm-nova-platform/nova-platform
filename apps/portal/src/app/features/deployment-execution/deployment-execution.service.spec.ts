import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { DeploymentExecutionService } from './deployment-execution.service';

describe('DeploymentExecutionService', () => {
  it('creates and starts executions', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'QUEUED' }));
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [DeploymentExecutionService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(DeploymentExecutionService);
    service
      .create({ releaseId: 'r1', environmentId: 'e1', provider: 'LOCAL' })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/deployment-executions/create', {
      releaseId: 'r1',
      environmentId: 'e1',
      provider: 'LOCAL',
    });
    service.start('x1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/deployment-executions/x1/start', {});
    service.logs('x1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/deployment-executions/x1/logs');
  });
});
