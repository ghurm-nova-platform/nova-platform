import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { OrganizationService } from './organization.service';

describe('OrganizationService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists organizations with search and pagination query params', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(OrganizationService);

    service.list({ search: 'nova', page: 1, size: 10, sort: 'name,asc' }).subscribe((page) => {
      expect(page.content.length).toBe(1);
    });

    const req = http.expectOne(
      (r) =>
        r.url === 'http://localhost:8080/api/organizations' &&
        r.params.get('search') === 'nova' &&
        r.params.get('page') === '1' &&
        r.params.get('size') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [
        {
          id: '11111111-1111-1111-1111-111111111111',
          name: 'Nova Demo Organization',
          slug: 'nova-demo',
          createdAt: '2026-07-17T00:00:00Z',
          updatedAt: '2026-07-17T00:00:00Z',
          createdBy: '44444444-4444-4444-4444-444444444401',
          updatedBy: '44444444-4444-4444-4444-444444444401',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 1,
    });
    http.verify();
  });
});
