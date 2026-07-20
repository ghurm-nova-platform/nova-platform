import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PolicyPage } from './policy-page';
import { PolicyPermissionHelper } from './policy-permission.helper';
import { PolicyService } from './policy.service';
import { Policy } from './policy.models';

describe('PolicyPage', () => {
  const policy: Policy = {
    id: 'p1',
    organizationId: '11111111-1111-1111-1111-111111111111',
    projectId: '55555555-5555-5555-5555-555555555501',
    policyName: 'Require SEMVER',
    description: 'test',
    policyType: 'SEMANTIC_VERSION_REQUIRED',
    status: 'ACTIVE',
    priority: 10,
    evaluationMode: 'ALL_REQUIRED',
    configuration: {},
    policyFingerprint: 'fp',
    createdBy: null,
    createdAt: '2026-07-20T00:00:00Z',
    updatedAt: '2026-07-20T00:01:00Z',
    latestEvaluation: {
      id: 'e1',
      releaseId: 'r1',
      decision: 'PASSED',
      evaluationHash: 'eh',
      summary: 'ok',
      completed: true,
      evidence: [
        {
          id: 'ev1',
          evidenceKey: 'semantic-version',
          evidenceType: 'RELEASE',
          referenceId: 'r1',
          passed: true,
          detail: 'valid',
          createdAt: '2026-07-20T00:01:00Z',
        },
      ],
      evaluatedAt: '2026-07-20T00:01:00Z',
    },
    versions: [
      {
        id: 'v1',
        versionNumber: 1,
        policyType: 'SEMANTIC_VERSION_REQUIRED',
        evaluationMode: 'ALL_REQUIRED',
        priority: 10,
        createdAt: '2026-07-20T00:00:00Z',
      },
    ],
    timeline: [
      { eventType: 'CREATED', at: '2026-07-20T00:00:00Z', detail: 'created' },
      { eventType: 'EVALUATION_COMPLETED', at: '2026-07-20T00:01:00Z', detail: 'done' },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PolicyPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['POLICY_RUN', 'POLICY_READ'],
            }),
          },
        },
        {
          provide: PolicyService,
          useValue: {
            create: () => of(policy),
            evaluate: () => of(policy),
            enable: () => of(policy),
            disable: () => of(policy),
            list: () => of([policy]),
            get: () => of(policy),
            history: () => of(policy),
          },
        },
        PolicyPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders safety statement', () => {
    const fixture = TestBed.createComponent(PolicyPage);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Release Policies');
    expect(text).toContain('do not modify releases');
  });

  it('shows decision badges, evidence, timeline, and history', () => {
    const fixture = TestBed.createComponent(PolicyPage);
    const page = fixture.componentInstance;
    page.selected.set(policy);
    page.policies.set([policy]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Require SEMVER');
    expect(text).toContain('PASSED');
    expect(text).toContain('semantic-version');
    expect(text).toContain('CREATED');
    expect(text).toContain('History');
    expect(text).toContain('ALL_REQUIRED');
  });
});
