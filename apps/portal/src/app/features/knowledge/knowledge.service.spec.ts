import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { KnowledgeService } from './knowledge.service';

describe('KnowledgeService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';
  const agentId = '66666666-6666-6666-6666-666666666601';
  const knowledgeBaseId = '88888888-8888-8888-8888-888888888801';
  const documentId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists knowledge bases with search and status filters through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(KnowledgeService);

    service
      .listKnowledgeBases(projectId, {
        search: 'docs',
        status: 'ACTIVE',
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === `http://localhost:8080/api/projects/${projectId}/knowledge-bases` &&
        r.params.get('search') === 'docs' &&
        r.params.get('status') === 'ACTIVE' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'createdAt,desc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('lists embedding providers through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(KnowledgeService);

    service.listProviders(projectId).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/knowledge-bases/providers`,
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ providers: [{ providerKey: 'DETERMINISTIC_LOCAL', model: 'deterministic-v1', dimensions: 64 }] });
    http.verify();
  });

  it('uploads a document through Platform API multipart only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(KnowledgeService);
    const formData = new FormData();
    formData.append('file', new Blob(['hello']), 'notes.txt');

    service.uploadDocument(projectId, knowledgeBaseId, formData).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeInstanceOf(FormData);
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: documentId,
      organizationId: '11111111-1111-1111-1111-111111111111',
      projectId,
      knowledgeBaseId,
      documentKey: 'NOTES',
      fileName: 'notes.txt',
      mediaType: 'text/plain',
      documentType: 'TEXT',
      status: 'READY',
      contentHash: 'abc123',
      fileSizeBytes: 5,
      extractedCharacterCount: 5,
      chunkCount: 1,
      ingestionErrorCode: null,
      version: 0,
      createdBy: '44444444-4444-4444-4444-444444444401',
      updatedBy: null,
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
      processedAt: '2026-07-17T00:00:00Z',
    });
    http.verify();
  });

  it('lists document chunks through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(KnowledgeService);

    service.listDocumentChunks(projectId, knowledgeBaseId, documentId, { page: 0, size: 20 }).subscribe();

    const req = http.expectOne(
      (r) =>
        r.url ===
          `http://localhost:8080/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/chunks` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '20',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });
    http.verify();
  });

  it('assigns a knowledge base to an agent through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(KnowledgeService);

    service.assignKnowledgeBase(projectId, agentId, { knowledgeBaseId }).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/agents/${agentId}/knowledge-bases`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ knowledgeBaseId });
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
      agentId,
      knowledgeBaseId,
      knowledgeKey: 'PRODUCT_DOCUMENTATION',
      knowledgeBaseName: 'Product documentation',
      knowledgeBaseStatus: 'ACTIVE',
      enabled: true,
      topKOverride: null,
      minimumScoreOverride: null,
      version: 0,
      createdBy: '44444444-4444-4444-4444-444444444401',
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:00:00Z',
    });
    http.verify();
  });

  it('reprocesses a document through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(KnowledgeService);

    service.reprocessDocument(projectId, knowledgeBaseId, documentId).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/reprocess`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      id: documentId,
      organizationId: '11111111-1111-1111-1111-111111111111',
      projectId,
      knowledgeBaseId,
      documentKey: 'NOTES',
      fileName: 'notes.txt',
      mediaType: 'text/plain',
      documentType: 'TEXT',
      status: 'READY',
      contentHash: 'abc123',
      fileSizeBytes: 5,
      extractedCharacterCount: 5,
      chunkCount: 1,
      ingestionErrorCode: null,
      version: 1,
      createdBy: '44444444-4444-4444-4444-444444444401',
      updatedBy: null,
      createdAt: '2026-07-17T00:00:00Z',
      updatedAt: '2026-07-17T00:01:00Z',
      processedAt: '2026-07-17T00:01:00Z',
    });
    http.verify();
  });
});
