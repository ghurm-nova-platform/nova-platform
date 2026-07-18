import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { CatalogModelService } from './catalog-model.service';

describe('CatalogModelService', () => {
  const modelId = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01';
  const providerId = 'cccccccc-cccc-cccc-cccc-cccccccccc01';
  const aliasId = 'dddddddd-dddd-dddd-dddd-dddddddddd01';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists catalog models with filters through Platform API', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(CatalogModelService);

    service
      .list({
        search: 'gpt',
        status: 'ACTIVE',
        source: 'PROVIDER_SYNC',
        capability: 'CHAT',
        providerId,
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === 'http://localhost:8080/api/ai-models' &&
        r.params.get('search') === 'gpt' &&
        r.params.get('status') === 'ACTIVE' &&
        r.params.get('source') === 'PROVIDER_SYNC' &&
        r.params.get('capability') === 'CHAT' &&
        r.params.get('providerId') === providerId &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'createdAt,desc',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('creates, updates, and runs lifecycle actions through Platform API', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(CatalogModelService);

    service
      .create({
        providerId,
        modelKey: 'GPT_4O',
        providerModelId: 'gpt-4o',
        displayName: 'GPT-4o',
        modelType: 'CHAT',
        contextWindowTokens: 128000,
        maxOutputTokens: 4096,
        capabilities: ['CHAT', 'STREAMING'],
      })
      .subscribe();
    const createReq = http.expectOne('http://localhost:8080/api/ai-models');
    expect(createReq.request.method).toBe('POST');
    expect(createReq.request.body.modelKey).toBe('GPT_4O');
    createReq.flush({ id: modelId, modelKey: 'GPT_4O', status: 'DRAFT', version: 0 });

    service
      .update(modelId, {
        version: 0,
        displayName: 'GPT-4o Updated',
        contextWindowTokens: 128000,
        maxOutputTokens: 4096,
      })
      .subscribe();
    const updateReq = http.expectOne(`http://localhost:8080/api/ai-models/${modelId}`);
    expect(updateReq.request.method).toBe('PUT');
    updateReq.flush({ id: modelId, displayName: 'GPT-4o Updated', version: 1 });

    service.activate(modelId).subscribe();
    http.expectOne(`http://localhost:8080/api/ai-models/${modelId}/activate`).flush({ id: modelId, status: 'ACTIVE' });

    service.disable(modelId).subscribe();
    http.expectOne(`http://localhost:8080/api/ai-models/${modelId}/disable`).flush({ id: modelId, status: 'DISABLED' });

    service.deprecate(modelId).subscribe();
    http
      .expectOne(`http://localhost:8080/api/ai-models/${modelId}/deprecate`)
      .flush({ id: modelId, status: 'DEPRECATED' });

    service.archive(modelId).subscribe();
    http.expectOne(`http://localhost:8080/api/ai-models/${modelId}/archive`).flush({ id: modelId, status: 'ARCHIVED' });

    http.verify();
  });

  it('manages capabilities, aliases, and provider sync through Platform API', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(CatalogModelService);

    service.get(modelId).subscribe();
    http.expectOne(`http://localhost:8080/api/ai-models/${modelId}`).flush({ id: modelId });

    service.replaceCapabilities(modelId, [{ capability: 'CHAT', enabled: true }]).subscribe();
    const capsReq = http.expectOne(`http://localhost:8080/api/ai-models/${modelId}/capabilities`);
    expect(capsReq.request.method).toBe('PUT');
    expect(capsReq.request.body).toEqual({ capabilities: [{ capability: 'CHAT', enabled: true }] });
    capsReq.flush({ id: modelId, capabilities: [] });

    service.listAliases(modelId).subscribe();
    http.expectOne(`http://localhost:8080/api/ai-models/${modelId}/aliases`).flush([]);

    service.createAlias(modelId, { alias: 'gpt-latest' }).subscribe();
    const aliasReq = http.expectOne(`http://localhost:8080/api/ai-models/${modelId}/aliases`);
    expect(aliasReq.request.method).toBe('POST');
    expect(aliasReq.request.body).toEqual({ alias: 'gpt-latest' });
    aliasReq.flush({ id: aliasId, modelId, alias: 'gpt-latest' });

    service.deleteAlias(aliasId).subscribe();
    const deleteReq = http.expectOne(`http://localhost:8080/api/ai-model-aliases/${aliasId}`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    service.syncModels(providerId).subscribe();
    const syncReq = http.expectOne(`http://localhost:8080/api/model-providers/${providerId}/models/sync`);
    expect(syncReq.request.method).toBe('POST');
    syncReq.flush({
      status: 'SUCCESS',
      errorCode: null,
      syncedAt: '2026-07-18T00:00:00Z',
      discoveredCount: 3,
      createdCount: 1,
      updatedCount: 1,
      unchangedCount: 1,
    });

    http.verify();
  });
});
