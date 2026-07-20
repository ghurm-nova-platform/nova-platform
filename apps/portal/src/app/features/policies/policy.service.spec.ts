import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PolicyService } from './policy.service';

describe('PolicyService', () => {
  it('posts create/evaluate and loads list/history', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'ACTIVE' }));
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [PolicyService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(PolicyService);
    service
      .create({
        projectId: 'proj',
        policyName: 'p',
        policyType: 'SEMANTIC_VERSION_REQUIRED',
      })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/policies', {
      projectId: 'proj',
      policyName: 'p',
      policyType: 'SEMANTIC_VERSION_REQUIRED',
    });
    service.evaluate('p1', { releaseId: 'r1' }).subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/policies/p1/evaluate', { releaseId: 'r1' });
    service.list('proj').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/policies?projectId=proj');
    service.history('p1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/policies/p1/history');
  });
});
