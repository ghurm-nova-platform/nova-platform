import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { AiModelService } from './ai-model.service';

describe('AiModelService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';
  const agentId = '66666666-6666-6666-6666-666666666601';
  const providerId = '99999999-9999-9999-9999-999999999901';
  const modelId = '99999999-9999-9999-9999-999999999911';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists provider models through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(AiModelService);

    service.listModels(providerId, { status: 'ACTIVE', page: 0, size: 10 }).subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === `http://localhost:8080/api/model-providers/${providerId}/models` &&
        r.params.get('status') === 'ACTIVE',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('assigns project models through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(AiModelService);

    service.assignProjectModel(projectId, { modelId, isDefault: true }).subscribe();

    const req = http.expectOne(`http://localhost:8080/api/projects/${projectId}/models`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ modelId, isDefault: true });
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ id: '99999999-9999-9999-9999-999999999921', modelId, isDefault: true });
    http.verify();
  });

  it('assigns agent models through Platform API only', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(AiModelService);

    service
      .assignAgentModel(projectId, agentId, {
        modelId,
        assignmentRole: 'PRIMARY',
        priority: 1,
      })
      .subscribe();

    const req = http.expectOne(`http://localhost:8080/api/projects/${projectId}/agents/${agentId}/models`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ modelId, assignmentRole: 'PRIMARY', priority: 1 });
    expect(req.request.url.includes('8090')).toBeFalse();
    req.flush({ id: '99999999-9999-9999-9999-999999999931', modelId, assignmentRole: 'PRIMARY' });
    http.verify();
  });
});
