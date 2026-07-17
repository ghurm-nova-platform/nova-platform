import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { AgentService } from './agent.service';

describe('AgentService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists agents with search and status filters', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(AgentService);

    service.list(projectId, { search: 'demo', status: 'ACTIVE', page: 0, size: 10 }).subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === `http://localhost:8080/api/projects/${projectId}/agents` &&
        r.params.get('search') === 'demo' &&
        r.params.get('status') === 'ACTIVE',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('patches agent status through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(AgentService);
    const agentId = '66666666-6666-6666-6666-666666666601';

    service.updateStatus(projectId, agentId, { status: 'PAUSED', version: 1 }).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/agents/${agentId}/status`,
    );
    expect(req.request.method).toBe('PATCH');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: agentId,
      organizationId: '11111111-1111-1111-1111-111111111111',
      projectId,
      name: 'Demo',
      description: null,
      systemPrompt: 'x',
      modelProvider: 'OPENAI',
      modelName: 'gpt',
      temperature: 0.2,
      maxTokens: 100,
      status: 'PAUSED',
      visibility: 'PROJECT',
      version: 2,
      createdBy: '44444444-4444-4444-4444-444444444401',
      updatedBy: '44444444-4444-4444-4444-444444444401',
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
    });
    http.verify();
  });
});
