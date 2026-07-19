import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CiService } from './ci.service';

describe('CiService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [CiService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(CiService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/ci/run', { taskId: 't1' });
  });

  it('loads latest CI observation by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [CiService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(CiService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/ci/t1');
  });

  it('loads CI observation history by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [CiService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(CiService);
    service.getHistory('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/ci/t1/history');
  });
});
