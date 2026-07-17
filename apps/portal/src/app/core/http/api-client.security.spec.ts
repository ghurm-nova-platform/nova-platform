import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { RuntimeConfigService } from '../config/runtime-config.service';
import { ApiClient } from './api-client';
import { correlationIdInterceptor } from './correlation-id.interceptor';
import { errorInterceptor } from './error.interceptor';

describe('Browser API security boundary', () => {
  it('does not load an apiKey or agentRuntimeUrl from runtime configuration', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService],
    });

    const http = TestBed.inject(HttpTestingController);
    const runtimeConfig = TestBed.inject(RuntimeConfigService);
    const loadPromise = runtimeConfig.load();

    const req = http.expectOne('/runtime-config.json');
    req.flush({
      platformApiUrl: 'http://localhost:8080',
      apiKey: 'should-be-ignored-if-present',
      agentRuntimeUrl: 'http://localhost:8090',
    });
    await loadPromise;

    const config = runtimeConfig.config() as unknown as Record<string, unknown>;
    expect(config['apiKey']).toBeUndefined();
    expect(config['agentRuntimeUrl']).toBeUndefined();
    expect(Object.keys(config)).toEqual(['platformApiUrl']);
    expect(runtimeConfig.platformApiUrl()).toBe('http://localhost:8080');
    expect('apiKey' in runtimeConfig).toBeFalse();
    http.verify();
  });

  it('does not attach an X-API-Key header from the browser client', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationIdInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        RuntimeConfigService,
        ApiClient,
      ],
    });

    const http = TestBed.inject(HttpTestingController);
    const api = TestBed.inject(ApiClient);

    api.get('/api/v1/health').subscribe();

    const req = http.expectOne('http://localhost:8080/api/v1/health');
    expect(req.request.headers.has('X-API-Key')).toBeFalse();
    expect(req.request.headers.has('X-Correlation-Id')).toBeTrue();
    expect(req.request.url.startsWith('http://localhost:8080')).toBeTrue();
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ status: 'UP' });
    http.verify();
  });
});
