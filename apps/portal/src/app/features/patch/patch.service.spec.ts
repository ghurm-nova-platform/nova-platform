import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PatchService } from './patch.service';

describe('PatchService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'VALID' }));
    TestBed.configureTestingModule({
      providers: [PatchService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(PatchService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/patch/run', { taskId: 't1' });
  });

  it('loads latest patch by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ status: 'VALID' }));
    TestBed.configureTestingModule({
      providers: [PatchService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(PatchService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/patch/t1');
  });
});
