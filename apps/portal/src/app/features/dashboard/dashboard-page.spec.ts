import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { DashboardPage } from './dashboard-page';
import { DashboardService } from './dashboard.service';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({
              roles: ['ORG_ADMIN'],
              permissions: ['DASHBOARD_READ', 'DASHBOARD_ADMIN'],
            }),
          },
        },
        {
          provide: DashboardService,
          useValue: {
            getConfig: () => of({ enabled: true, refreshRateSeconds: 30, cacheTtlSeconds: 30 }),
            getSnapshot: () =>
              of({
                meta: {
                  organizationId: 'org',
                  generatedAt: new Date().toISOString(),
                  cacheExpiresAt: new Date().toISOString(),
                  refreshRateSeconds: 30,
                  fromCache: false,
                },
                overview: {
                  projectCount: 1,
                  agentCount: 2,
                  activeRunCount: 0,
                  totalRunCount: 0,
                  releaseCount: 0,
                  deploymentCount: 0,
                  executionCount: 0,
                  environmentCount: 0,
                  auditEventCount: 0,
                  pendingApprovalCount: 0,
                  failedCiCount: 0,
                  rollbackReadyCount: 0,
                  kpis: {
                    releaseSuccessRate: 0,
                    deploymentSuccessRate: 0,
                    pipelineSuccessRate: 0,
                    approvalSlaComplianceRate: 0,
                    ciPassRate: 0,
                    rollbackReadinessRate: 0,
                    avgReleaseDurationMs: 0,
                    avgDeploymentDurationMs: 0,
                    avgPipelineStageDurationMs: 0,
                    avgApprovalWaitMs: 0,
                    avgCiDurationMs: 0,
                    avgRollbackPlanDurationMs: 0,
                  },
                },
                pipeline: { stages: [], totalActiveTasks: 0 },
                deployments: { running: [], totalRunning: 0, totalCompleted: 0, totalFailed: 0 },
                releases: {
                  published: 0,
                  ready: 0,
                  blocked: 0,
                  pendingApproval: 0,
                  policyFailures: 0,
                  rollbackReady: 0,
                  recent: [],
                },
                environments: { buckets: [], environments: [] },
                audit: { events: [], total: 0 },
                approvals: { waiting: 0, expired: 0, blocked: 0, slaBreaches: 0, queue: [] },
                ci: { recentPipelines: [], failedBuilds: 0, repairRequests: 0, queueDepth: 0, avgDurationMs: 0 },
                rollbacks: {
                  ready: 0,
                  executed: 0,
                  failed: 0,
                  coveragePercent: 0,
                  avgDurationMs: 0,
                  recent: [],
                },
                cost: {
                  estimatedTotalCost: 0,
                  providerUsage: [],
                  futureLlmCostEstimate: 0,
                  note: 'placeholder',
                },
              }),
            refresh: () => of({ refreshedAt: new Date().toISOString(), cacheExpiresAt: new Date().toISOString() }),
            exportUrl: () => '/api/dashboard/export?format=csv&section=overview',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
  });

  it('renders dashboard heading', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Enterprise Dashboard');
  });
});
