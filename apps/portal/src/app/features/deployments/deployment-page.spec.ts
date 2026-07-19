import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { DeploymentPage } from './deployment-page';
import { DeploymentPermissionHelper } from './deployment-permission.helper';
import { DeploymentService } from './deployment.service';
import { Deployment } from './deployment.models';

describe('DeploymentPage', () => {
  const deployment: Deployment = {
    id: 'd1',
    organizationId: '11111111-1111-1111-1111-111111111111',
    projectId: '55555555-5555-5555-5555-555555555501',
    releaseId: 'r1',
    environmentId: 'e1',
    environmentCode: 'STAGING',
    environmentName: 'Staging',
    customEnvironmentName: null,
    semanticVersion: '1.2.3',
    releaseManifestHash: 'abc',
    status: 'SUCCEEDED',
    health: 'HEALTHY',
    healthMessage: 'ok',
    deploymentProvider: 'LOCAL',
    externalDeploymentKey: 'k1',
    deploymentHash: 'hash',
    triggeredBy: null,
    startedAt: '2026-07-19T00:00:00Z',
    finishedAt: '2026-07-19T00:01:00Z',
    durationMs: 60000,
    logMetadata: null,
    errorCode: null,
    errorMessage: null,
    artifacts: [],
    healthHistory: [{ id: 'h1', health: 'HEALTHY', message: 'ok', observedAt: '2026-07-19T00:00:00Z' }],
    timeline: [
      { eventType: 'OBSERVED', at: '2026-07-19T00:00:00Z', detail: 'observed' },
      { eventType: 'VERIFY_PASSED', at: '2026-07-19T00:01:00Z', detail: 'verified' },
    ],
    createdAt: '2026-07-19T00:00:00Z',
    updatedAt: '2026-07-19T00:01:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeploymentPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['DEPLOYMENT_RUN', 'DEPLOYMENT_READ'],
            }),
          },
        },
        {
          provide: DeploymentService,
          useValue: {
            observe: () => of(deployment),
            verify: () => of(deployment),
            list: () => of([deployment]),
            get: () => of(deployment),
            history: () => of(deployment),
            environments: () =>
              of([
                {
                  id: 'e1',
                  code: 'STAGING',
                  name: 'Staging',
                  environmentType: 'STAGING',
                  sortOrder: 40,
                  active: true,
                },
              ]),
          },
        },
        DeploymentPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders safety statement and environments', () => {
    const fixture = TestBed.createComponent(DeploymentPage);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Deployment Observation');
    expect(text).toContain('does not deploy');
    expect(text).toContain('Staging');
  });

  it('shows version, health, provider, timeline, and history', () => {
    const fixture = TestBed.createComponent(DeploymentPage);
    const page = fixture.componentInstance;
    page.selected.set(deployment);
    page.deployments.set([deployment]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('1.2.3');
    expect(text).toContain('HEALTHY');
    expect(text).toContain('LOCAL');
    expect(text).toContain('OBSERVED');
    expect(text).toContain('History');
  });
});
