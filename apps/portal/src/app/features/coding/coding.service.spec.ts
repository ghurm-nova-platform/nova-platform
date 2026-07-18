import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CodingService } from './coding.service';

describe('CodingService', () => {
  it('posts generate requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ validated: true, artifacts: [] }));
    TestBed.configureTestingModule({
      providers: [CodingService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(CodingService);
    service.generate({ taskId: 't1' }).subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/coding/generate', { taskId: 't1' });
  });

  it('loads artifacts by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [CodingService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(CodingService);
    service.listArtifacts('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/coding/artifacts/t1');
  });
});
