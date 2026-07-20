import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { EnvironmentService } from './environment.service';

describe('EnvironmentService', () => {
  let service: EnvironmentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EnvironmentService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EnvironmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists environments by project', () => {
    service.list('55555555-5555-5555-5555-555555555501').subscribe();
    const req = http.expectOne((r) => r.url.includes('/api/environments'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
