import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuditService } from './audit.service';

describe('AuditService', () => {
  let service: AuditService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuditService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuditService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists recent audit events', () => {
    service.list({ page: 0, size: 20 }).subscribe();
    const req = http.expectOne((r) => r.url.includes('/api/audit'));
    expect(req.request.method).toBe('GET');
    req.flush({ events: [], total: 0, page: 0, size: 20 });
  });

  it('searches audit events', () => {
    service.search({ entityType: 'ENVIRONMENT', page: 0, size: 10 }).subscribe();
    const req = http.expectOne((r) => r.url.includes('/api/audit/search'));
    expect(req.request.method).toBe('GET');
    req.flush({ events: [], total: 0, page: 0, size: 10 });
  });
});
