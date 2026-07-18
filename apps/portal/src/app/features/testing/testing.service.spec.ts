import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { TestingService } from './testing.service';

describe('TestingService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ coverageEstimate: 80 }));
    TestBed.configureTestingModule({
      providers: [TestingService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(TestingService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/testing/run', { taskId: 't1' });
  });

  it('loads latest testing result by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ coverageEstimate: 70 }));
    TestBed.configureTestingModule({
      providers: [TestingService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(TestingService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/testing/t1');
  });
});
