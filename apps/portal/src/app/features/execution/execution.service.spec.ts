import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { ExecutionService } from './execution.service';

describe('ExecutionService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';
  const agentId = '66666666-6666-6666-6666-666666666601';
  const executionId = '77777777-7777-7777-7777-777777777701';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('executes an agent through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ExecutionService);
    const conversationId = '88888888-8888-8888-8888-888888888801';
    const clientRequestId = '99999999-9999-9999-9999-999999999901';

    service
      .execute(projectId, agentId, {
        input: { message: 'Hello agent' },
        variables: { name: 'Nova' },
        conversationId,
        clientRequestId,
      })
      .subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/agents/${agentId}/execute`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      input: { message: 'Hello agent' },
      variables: { name: 'Nova' },
      conversationId,
      clientRequestId,
    });
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      executionId,
      status: 'COMPLETED',
      response: 'Hi there',
      latencyMs: 120,
      tokens: { input: 10, output: 5, total: 15 },
      renderedPrompt: 'System: You are helpful',
    });
    http.verify();
  });

  it('lists executions with agentId filter through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ExecutionService);

    service.list(projectId, { agentId, page: 0, size: 10, sort: 'createdAt,desc' }).subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === `http://localhost:8080/api/projects/${projectId}/executions` &&
        r.params.get('agentId') === agentId &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'createdAt,desc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });
});
