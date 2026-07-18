import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { GitService } from './git.service';

describe('GitService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [GitService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(GitService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/git/run', { taskId: 't1' });
  });

  it('loads latest git operation by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [GitService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(GitService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/git/t1');
  });
});
