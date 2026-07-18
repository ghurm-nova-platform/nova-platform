import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { PlannerService } from './planner.service';

describe('PlannerService', () => {
  it('posts plan requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ validated: true }));
    TestBed.configureTestingModule({
      providers: [PlannerService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(PlannerService);
    service.plan({ projectId: 'p1', objective: 'x' }).subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/planner/plan', {
      projectId: 'p1',
      objective: 'x',
    });
  });
});
