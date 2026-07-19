import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { MergeService } from './merge.service';

describe('MergeService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [MergeService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(MergeService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/merge/run', { taskId: 't1' });
  });

  it('loads latest merge operation by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [MergeService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(MergeService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/merge/t1');
  });

  it('loads merge history by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [MergeService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(MergeService);
    service.getHistory('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/merge/t1/history');
  });
});
