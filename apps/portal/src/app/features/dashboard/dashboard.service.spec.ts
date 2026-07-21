import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads dashboard snapshot', () => {
    service.getSnapshot().subscribe();
    const req = http.expectOne((r) => r.url.includes('/api/dashboard'));
    expect(req.request.method).toBe('GET');
    req.flush({ meta: {}, overview: {}, pipeline: { stages: [] } });
  });

  it('builds export url', () => {
    expect(service.exportUrl('csv', 'overview')).toContain('format=csv');
    expect(service.exportUrl('pdf', 'pipeline', 'abc')).toContain('projectId=abc');
  });
});
