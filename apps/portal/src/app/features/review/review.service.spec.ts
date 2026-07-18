import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { ReviewService } from './review.service';

describe('ReviewService', () => {
  it('posts run requests', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.post.and.returnValue(of({ score: 90, approved: true }));
    TestBed.configureTestingModule({
      providers: [ReviewService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ReviewService);
    service.run('t1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/review/run', { taskId: 't1' });
  });

  it('loads latest review by task id', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['post', 'get']);
    api.get.and.returnValue(of({ score: 88 }));
    TestBed.configureTestingModule({
      providers: [ReviewService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(ReviewService);
    service.getLatest('t1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/review/t1');
  });
});
