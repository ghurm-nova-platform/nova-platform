import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { PromptService } from './prompt.service';

describe('PromptService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists prompts with search, status, type, and tag filters', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(PromptService);

    service
      .list(projectId, {
        search: 'welcome',
        status: 'PUBLISHED',
        type: 'CHAT',
        tag: 'onboarding',
        page: 0,
        size: 10,
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === `http://localhost:8080/api/projects/${projectId}/prompts` &&
        r.params.get('search') === 'welcome' &&
        r.params.get('status') === 'PUBLISHED' &&
        r.params.get('type') === 'CHAT' &&
        r.params.get('tag') === 'onboarding',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('publishes a draft version through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(PromptService);
    const promptId = '77777777-7777-7777-7777-777777777701';
    const versionId = '88888888-8888-8888-8888-888888888801';

    service.publishVersion(projectId, promptId, versionId, { reason: 'Ready' }).subscribe();

    const req = http.expectOne(
      `http://localhost:8080/api/projects/${projectId}/prompts/${promptId}/versions/${versionId}/publish`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.url.includes('8090')).toBeFalse();
    expect(req.request.body).toEqual({ reason: 'Ready' });
    req.flush({
      id: versionId,
      promptId,
      versionNumber: 2,
      content: 'Hello {{name}}',
      changeSummary: 'Publish',
      status: 'PUBLISHED',
      variables: [],
      createdBy: '44444444-4444-4444-4444-444444444401',
      createdAt: '2026-07-17T00:00:00Z',
      publishedBy: '44444444-4444-4444-4444-444444444401',
      publishedAt: '2026-07-17T01:00:00Z',
    });
    http.verify();
  });

  it('previews prompt content through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(PromptService);

    service
      .preview(projectId, {
        content: 'Hello {{name}}',
        variables: [
          {
            name: 'name',
            dataType: 'STRING',
            required: true,
          },
        ],
        values: { name: 'Nova' },
      })
      .subscribe();

    const req = http.expectOne(`http://localhost:8080/api/projects/${projectId}/prompts/preview`);
    expect(req.request.method).toBe('POST');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      renderedContent: 'Hello Nova',
      errors: [],
      warnings: [],
      missingRequiredVariables: [],
    });
    http.verify();
  });
});
