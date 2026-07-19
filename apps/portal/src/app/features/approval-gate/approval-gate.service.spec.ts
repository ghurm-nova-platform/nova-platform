import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { ApprovalGateService } from './approval-gate.service';

describe('ApprovalGateService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ decision: 'PENDING' }));
    TestBed.configureTestingModule({
      providers: [ApprovalGateService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ApprovalGateService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/approval-gate/run', { taskId: 't1' });
  });

  it('loads latest approval decision by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ decision: 'APPROVED' }));
    TestBed.configureTestingModule({
      providers: [ApprovalGateService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ApprovalGateService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/approval-gate/t1');
  });

  it('loads approval history by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [ApprovalGateService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ApprovalGateService);
    service.getHistory('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/approval-gate/t1/history');
  });

  it('loads requirements by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [ApprovalGateService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ApprovalGateService);
    service.getRequirements('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/approval-gate/t1/requirements');
  });

  it('posts approve requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ decision: 'APPROVED' }));
    TestBed.configureTestingModule({
      providers: [ApprovalGateService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ApprovalGateService);
    service.approve('t1', { comment: 'ok' }).subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/approval-gate/t1/approve', { comment: 'ok' });
  });

  it('posts reject requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ decision: 'REJECTED' }));
    TestBed.configureTestingModule({
      providers: [ApprovalGateService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ApprovalGateService);
    service.reject('t1', { comment: 'needs work' }).subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/approval-gate/t1/reject', {
      comment: 'needs work',
    });
  });
});
