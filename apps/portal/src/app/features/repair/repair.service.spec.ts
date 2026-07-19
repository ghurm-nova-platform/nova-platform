import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { RepairService } from './repair.service';

describe('RepairService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [RepairService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(RepairService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/repair/run', { taskId: 't1' });
  });

  it('loads latest repair operation by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ status: 'SUCCEEDED' }));
    TestBed.configureTestingModule({
      providers: [RepairService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(RepairService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/repair/t1');
  });

  it('loads repair history by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [RepairService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(RepairService);
    service.getHistory('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/repair/t1/history');
  });
});
