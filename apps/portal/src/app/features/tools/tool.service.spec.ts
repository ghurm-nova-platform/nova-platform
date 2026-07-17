import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { ToolService } from './tool.service';

describe('ToolService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';
  const agentId = '66666666-6666-6666-6666-666666666601';
  const toolId = '77777777-7777-7777-7777-777777777701';
  const executionId = '99999999-9999-9999-9999-999999999901';
  const toolCallId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists tools with search, status, and type filters', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ToolService);

    service
      .listTools(projectId, {
        search: 'calc',
        status: 'ACTIVE',
        type: 'BUILT_IN',
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === `http://localhost:8080/api/projects/${projectId}/tools` &&
        r.params.get('search') === 'calc' &&
        r.params.get('status') === 'ACTIVE' &&
        r.params.get('type') === 'BUILT_IN' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'createdAt,desc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('lists executors through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ToolService);

    service.listExecutors(projectId).subscribe();

    const req = http.expectOne(`http://localhost:8080/api/projects/${projectId}/tools/executors`);
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ executorKeys: ['CALCULATOR', 'CURRENT_DATETIME'] });
    http.verify();
  });

  it('assigns a tool to an agent through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ToolService);

    service.assignTool(projectId, agentId, { toolId }).subscribe();

    const req = http.expectOne(`http://localhost:8080/api/projects/${projectId}/agents/${agentId}/tools`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ toolId });
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
      agentId,
      toolId,
      toolKey: 'CALCULATOR',
      toolName: 'Calculator',
      toolStatus: 'ACTIVE',
      enabled: true,
      version: 0,
      createdBy: '44444444-4444-4444-4444-444444444401',
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
    });
    http.verify();
  });

  it('lists execution tool calls through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ToolService);

    service.listExecutionToolCalls(projectId, executionId).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/executions/${executionId}/tool-calls`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush([]);
    http.verify();
  });

  it('approves a tool call through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ToolService);

    service.approveToolCall(projectId, executionId, toolCallId, { version: 0 }).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/executions/${executionId}/tool-calls/${toolCallId}/approve`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ version: 0 });
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: toolCallId,
      executionId,
      agentId,
      toolId,
      toolKey: 'CALCULATOR',
      runtimeCallId: 'call-1',
      sequenceNumber: 1,
      status: 'APPROVED',
      inputPayload: '{}',
      outputPayload: null,
      errorCode: null,
      requestedAt: '2026-07-17T00:00:00Z',
      startedAt: null,
      completedAt: null,
      durationMs: null,
      approvedBy: '44444444-4444-4444-4444-444444444401',
      approvedAt: '2026-07-17T00:01:00Z',
      createdBy: '44444444-4444-4444-4444-444444444401',
    });
    http.verify();
  });

  it('continues execution through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ToolService);

    service.continueExecution(projectId, executionId).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/executions/${executionId}/continue`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ executionId, readyToContinue: true, message: 'Ready to continue execution' });
    http.verify();
  });
});
