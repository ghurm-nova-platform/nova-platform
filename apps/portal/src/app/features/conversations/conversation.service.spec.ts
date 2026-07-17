import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { ConversationService } from './conversation.service';

describe('ConversationService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';
  const agentId = '66666666-6666-6666-6666-666666666601';
  const conversationId = '88888888-8888-8888-8888-888888888801';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists conversations with agentId, status, and search filters', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ConversationService);

    service
      .list(projectId, {
        agentId,
        status: 'ACTIVE',
        search: 'support',
        page: 0,
        size: 10,
        sort: 'lastMessageAt,desc',
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === `http://localhost:8080/api/projects/${projectId}/conversations` &&
        r.params.get('agentId') === agentId &&
        r.params.get('status') === 'ACTIVE' &&
        r.params.get('search') === 'support' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'lastMessageAt,desc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('creates a conversation through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ConversationService);

    service.create(projectId, { agentId, title: 'Support chat' }).subscribe();

    const req = http.expectOne(`http://localhost:8080/api/projects/${projectId}/conversations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ agentId, title: 'Support chat' });
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: conversationId,
      projectId,
      agentId,
      title: 'Support chat',
      status: 'ACTIVE',
      messageCount: 0,
      lastMessageAt: null,
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
      version: 0,
    });
    http.verify();
  });

  it('restores an archived conversation through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ConversationService);

    service.restore(projectId, conversationId).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/conversations/${conversationId}/restore`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: conversationId,
      projectId,
      agentId,
      title: 'Support chat',
      status: 'ACTIVE',
      messageCount: 2,
      lastMessageAt: '2026-07-17T01:00:00Z',
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T01:00:00Z',
      version: 1,
    });
    http.verify();
  });

  it('lists conversation messages through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ConversationService);

    service
      .listMessages(projectId, conversationId, { page: 0, size: 20, sort: 'sequenceNumber,asc' })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url ===
          `http://localhost:8080/api/projects/${projectId}/conversations/${conversationId}/messages` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '20' &&
        r.params.get('sort') === 'sequenceNumber,asc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });
    http.verify();
  });
});
