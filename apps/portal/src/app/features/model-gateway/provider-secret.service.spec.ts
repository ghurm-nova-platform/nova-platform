import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { ProviderSecretService } from './provider-secret.service';

describe('ProviderSecretService', () => {
  const secretId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
    sessionStorage.clear();
    localStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it('lists secrets through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ProviderSecretService);

    service
      .listSecrets({
        search: 'openai',
        status: 'ACTIVE',
        providerType: 'OPENAI',
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === 'http://localhost:8080/api/provider-secrets' &&
        r.params.get('search') === 'openai' &&
        r.params.get('status') === 'ACTIVE' &&
        r.params.get('providerType') === 'OPENAI' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'createdAt,desc',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('creates a secret through Platform API and never persists plaintext in browser storage', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ProviderSecretService);
    const plaintext = 'sk-test-plaintext-secret-value-123456';

    service
      .createSecret({
        secretKey: 'OPENAI_PROD',
        name: 'OpenAI Prod',
        providerType: 'OPENAI',
        secret: plaintext,
      })
      .subscribe((response) => {
        expect((response as { secret?: string }).secret).toBeUndefined();
        expect(response.credentialReference).toBe(`vault:provider-secret:${secretId}`);
      });

    const req = http.expectOne('http://localhost:8080/api/provider-secrets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.secret).toBe(plaintext);
    req.flush({
      id: secretId,
      secretKey: 'OPENAI_PROD',
      name: 'OpenAI Prod',
      description: null,
      providerType: 'OPENAI',
      status: 'ACTIVE',
      credentialReference: `vault:provider-secret:${secretId}`,
      algorithm: 'AES-256-GCM',
      keyVersion: 1,
      fingerprintSha256: 'abc',
      last4: '3456',
      version: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
      rotatedAt: null,
      revokedAt: null,
    });

    expect(sessionStorage.length).toBe(0);
    expect(localStorage.length).toBe(0);
    expect(JSON.stringify(sessionStorage)).not.toContain(plaintext);
    expect(JSON.stringify(localStorage)).not.toContain(plaintext);
    http.verify();
  });

  it('gets, rotates, and revokes secrets through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ProviderSecretService);

    service.getSecret(secretId).subscribe();
    const getReq = http.expectOne(`http://localhost:8080/api/provider-secrets/${secretId}`);
    expect(getReq.request.method).toBe('GET');
    getReq.flush({ id: secretId, status: 'ACTIVE', credentialReference: `vault:provider-secret:${secretId}` });

    service.rotateSecret(secretId, { secret: 'new-secret-value' }).subscribe();
    const rotateReq = http.expectOne(`http://localhost:8080/api/provider-secrets/${secretId}/rotate`);
    expect(rotateReq.request.method).toBe('POST');
    expect(rotateReq.request.body).toEqual({ secret: 'new-secret-value' });
    rotateReq.flush({ id: secretId, status: 'ACTIVE' });

    service.revokeSecret(secretId).subscribe();
    const revokeReq = http.expectOne(`http://localhost:8080/api/provider-secrets/${secretId}/revoke`);
    expect(revokeReq.request.method).toBe('POST');
    revokeReq.flush({ id: secretId, status: 'REVOKED' });

    expect(sessionStorage.length).toBe(0);
    expect(localStorage.length).toBe(0);
    http.verify();
  });
});
