import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { RollbackService } from './rollback.service';

describe('RollbackService', () => {
  it('posts create and loads list/history', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'DRAFT' }));
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [RollbackService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(RollbackService);
    service
      .create({
        releaseId: 'r1',
        deploymentId: 'd1',
        targetReleaseId: 'r0',
        environment: 'STAGING',
        strategy: 'PREVIOUS_RELEASE',
      })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/rollbacks/create', {
      releaseId: 'r1',
      deploymentId: 'd1',
      targetReleaseId: 'r0',
      environment: 'STAGING',
      strategy: 'PREVIOUS_RELEASE',
    });
    service.list('proj').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/rollbacks?projectId=proj');
    service.history('rb1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/rollbacks/rb1/history');
  });
});
