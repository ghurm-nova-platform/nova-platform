import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { ModelProviderService } from './model-provider.service';

describe('ModelProviderService', () => {
  const providerId = '99999999-9999-9999-9999-999999999901';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists providers through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ModelProviderService);

    service
      .listProviders({
        search: 'nova',
        status: 'ACTIVE',
        providerType: 'DETERMINISTIC_LOCAL',
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === 'http://localhost:8080/api/model-providers' &&
        r.params.get('search') === 'nova' &&
        r.params.get('status') === 'ACTIVE' &&
        r.params.get('providerType') === 'DETERMINISTIC_LOCAL' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'createdAt,desc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('creates a provider through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ModelProviderService);

    service
      .createProvider({
        providerKey: 'NOVA_LOCAL',
        name: 'Nova Local',
        providerType: 'DETERMINISTIC_LOCAL',
        adapterKey: 'DETERMINISTIC_LOCAL',
        requestTimeoutSeconds: 30,
        maxConcurrentRequests: 10,
        maxRetries: 1,
        retryBackoffMs: 250,
      })
      .subscribe();

    const req = http.expectOne('http://localhost:8080/api/model-providers');
    expect(req.request.method).toBe('POST');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ id: providerId, providerKey: 'NOVA_LOCAL', status: 'DRAFT' });
    http.verify();
  });

  it('activates and disables providers through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ModelProviderService);

    service.activateProvider(providerId).subscribe();
    const activateReq = http.expectOne(`http://localhost:8080/api/model-providers/${providerId}/activate`);
    expect(activateReq.request.method).toBe('POST');
    activateReq.flush({ id: providerId, status: 'ACTIVE' });

    service.disableProvider(providerId).subscribe();
    const disableReq = http.expectOne(`http://localhost:8080/api/model-providers/${providerId}/disable`);
    expect(disableReq.request.method).toBe('POST');
    disableReq.flush({ id: providerId, status: 'DISABLED' });
    http.verify();
  });

  it('lists adapter keys through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ModelProviderService);

    service.listAdapters().subscribe();

    const req = http.expectOne('http://localhost:8080/api/model-providers/adapters');
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({
      adapters: [
        {
          adapterKey: 'DETERMINISTIC_LOCAL',
          tools: true,
          knowledgeContext: true,
          jsonOutput: true,
          systemMessages: true,
          streaming: false,
        },
      ],
    });
    http.verify();
  });

  it('tests provider connection through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ModelProviderService);

    service.testConnection(providerId).subscribe();

    const req = http.expectOne(`http://localhost:8080/api/model-providers/${providerId}/connection-test`);
    expect(req.request.method).toBe('POST');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ status: 'SUCCESS', errorCode: null, testedAt: '2026-01-01T00:00:00Z' });
    http.verify();
  });
});
