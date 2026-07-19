import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PullRequestService } from './pull-request.service';

describe('PullRequestService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [PullRequestService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(PullRequestService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/pull-requests/run', { taskId: 't1' });
  });

  it('loads latest pull request operation by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [PullRequestService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(PullRequestService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/pull-requests/t1');
  });

  it('loads pull request history by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [PullRequestService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(PullRequestService);
    service.getHistory('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/pull-requests/t1/history');
  });
});
