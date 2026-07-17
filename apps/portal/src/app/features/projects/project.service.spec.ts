import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { ProjectService } from './project.service';

describe('ProjectService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('creates a project with typed payload', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ProjectService);

    service
      .create({
        name: 'Portal',
        description: 'UI',
        status: 'ACTIVE',
        visibility: 'PRIVATE',
      })
      .subscribe((project) => {
        expect(project.name).toBe('Portal');
      });

    const req = http.expectOne('http://localhost:8080/api/projects');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      name: 'Portal',
      description: 'UI',
      status: 'ACTIVE',
      visibility: 'PRIVATE',
    });
    req.flush({
      id: '55555555-5555-5555-5555-555555555599',
      organizationId: '11111111-1111-1111-1111-111111111111',
      name: 'Portal',
      description: 'UI',
      status: 'ACTIVE',
      visibility: 'PRIVATE',
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
      createdBy: '44444444-4444-4444-4444-444444444401',
      updatedBy: '44444444-4444-4444-4444-444444444401',
    });
    http.verify();
  });
});
