import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { RollbackPage } from './rollback-page';
import { RollbackPermissionHelper } from './rollback-permission.helper';
import { RollbackService } from './rollback.service';
import { Rollback } from './rollback.models';

describe('RollbackPage', () => {
  const rollback: Rollback = {
    id: 'rb1',
    organizationId: '11111111-1111-1111-1111-111111111111',
    projectId: '55555555-5555-5555-5555-555555555501',
    releaseId: 'r1',
    deploymentId: 'd1',
    targetReleaseId: 'r0',
    currentVersion: '2.0.0',
    targetVersion: '1.9.0',
    environmentId: 'e1',
    environmentCode: 'STAGING',
    status: 'READY',
    strategy: 'PREVIOUS_RELEASE',
    rollbackPlanHash: 'planhash',
    createdBy: null,
    createdAt: '2026-07-19T00:00:00Z',
    validatedAt: '2026-07-19T00:01:00Z',
    updatedAt: '2026-07-19T00:01:00Z',
    errorCode: null,
    errorMessage: null,
    plan: {
      id: 'p1',
      currentReleaseId: 'r1',
      targetReleaseId: 'r0',
      deploymentId: 'd1',
      environmentCode: 'STAGING',
      strategy: 'PREVIOUS_RELEASE',
      reason: 'regression',
      riskLevel: 'MEDIUM',
      validationResult: 'PASSED',
      validationMessage: 'ok',
      immutable: true,
      createdAt: '2026-07-19T00:00:00Z',
      updatedAt: '2026-07-19T00:01:00Z',
    },
    targets: [
      {
        id: 't1',
        targetReleaseId: 'r0',
        targetVersion: '1.9.0',
        sortOrder: 0,
        createdAt: '2026-07-19T00:00:00Z',
      },
    ],
    validations: [
      {
        id: 'v1',
        checkCode: 'VERSION_COMPATIBILITY',
        passed: true,
        message: 'ok',
        createdAt: '2026-07-19T00:01:00Z',
      },
    ],
    timeline: [
      { eventType: 'CREATED', at: '2026-07-19T00:00:00Z', detail: 'created' },
      { eventType: 'READY', at: '2026-07-19T00:01:00Z', detail: 'ready' },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RollbackPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['ROLLBACK_RUN', 'ROLLBACK_READ'],
            }),
          },
        },
        {
          provide: RollbackService,
          useValue: {
            create: () => of(rollback),
            validate: () => of(rollback),
            list: () => of([rollback]),
            get: () => of(rollback),
            history: () => of(rollback),
          },
        },
        RollbackPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders safety statement', () => {
    const fixture = TestBed.createComponent(RollbackPage);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Rollback Manager');
    expect(text).toContain('does not execute rollback');
  });

  it('shows versions, strategy, validation badges, timeline, and history', () => {
    const fixture = TestBed.createComponent(RollbackPage);
    const page = fixture.componentInstance;
    page.selected.set(rollback);
    page.rollbacks.set([rollback]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('2.0.0');
    expect(text).toContain('1.9.0');
    expect(text).toContain('PREVIOUS_RELEASE');
    expect(text).toContain('PASSED');
    expect(text).toContain('CREATED');
    expect(text).toContain('History');
    expect(text).toContain('planhash');
  });
});
