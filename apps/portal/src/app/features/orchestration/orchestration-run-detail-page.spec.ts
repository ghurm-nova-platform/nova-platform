import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { convertToParamMap } from '@angular/router';

import { UserSessionService } from '../../auth/services/user-session.service';
import { AuthUser } from '../../auth/services/auth.models';
import { OrchestrationRunDetailPage } from './orchestration-run-detail-page';

describe('OrchestrationRunDetailPage', () => {
  const runId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrchestrationRunDetailPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ runId }),
              url: [],
            },
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    TestBed.inject(UserSessionService).clear();
  });

  it('stops polling on destroy', () => {
    const session = TestBed.inject(UserSessionService);
    const admin: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444401',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'admin@nova.local',
      displayName: 'Nova Admin',
      roles: ['ORG_ADMIN'],
      permissions: [],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, admin);

    const fixture = TestBed.createComponent(OrchestrationRunDetailPage);
    const component = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    const getReq = http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}`);
    getReq.flush({
      id: runId,
      organizationId: admin.organizationId,
      projectId: '55555555-5555-5555-5555-555555555501',
      initiatedByAgentId: null,
      rootExecutionId: null,
      name: 'Running run',
      objective: 'Test',
      status: 'RUNNING',
      executionMode: 'SEQUENTIAL',
      failurePolicy: 'FAIL_FAST',
      maxParallelTasks: 2,
      maximumDurationMs: 3600000,
      startedAt: '2026-07-18T00:00:00Z',
      completedAt: null,
      cancelledAt: null,
      deadlineAt: null,
      cancellationReason: null,
      failureCode: null,
      failureMessage: null,
      inputJson: null,
      outputJson: null,
      metadataJson: null,
      createdBy: admin.userId,
      updatedBy: admin.userId,
      createdAt: '2026-07-18T00:00:00Z',
      updatedAt: '2026-07-18T00:00:00Z',
      version: 1,
      taskStatusCounts: {},
      runningTaskCount: 1,
      completedPercentage: 10,
    });

    http.match(`http://localhost:8080/api/orchestration-runs/${runId}/tasks`).forEach((req) =>
      req.flush({ content: [], totalElements: 0, totalPages: 0, size: 100, number: 0 }),
    );
    http.match(`http://localhost:8080/api/orchestration-runs/${runId}/events`).forEach((req) =>
      req.flush({ content: [], totalElements: 0, totalPages: 0, size: 50, number: 0 }),
    );

    expect((component as unknown as { pollSub: unknown }).pollSub).toBeTruthy();
    fixture.destroy();
    expect((component as unknown as { pollSub: unknown }).pollSub).toBeNull();
  });
});
