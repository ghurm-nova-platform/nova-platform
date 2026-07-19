import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { ReleaseService } from './release.service';

describe('ReleaseService', () => {
  it('posts create requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ status: 'DRAFT' }));
    TestBed.configureTestingModule({
      providers: [ReleaseService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ReleaseService);
    service.create({ projectId: 'p1', releaseName: 'r1' }).subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/releases/create', {
      projectId: 'p1',
      releaseName: 'r1',
    });
  });

  it('loads release list and history', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [ReleaseService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ReleaseService);
    service.list('proj').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/releases?projectId=proj');
    service.history('rel1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/releases/rel1/history');
  });
});
