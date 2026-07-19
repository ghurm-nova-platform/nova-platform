import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { ApprovalGatePage } from './approval-gate-page';
import { ApprovalGatePermissionHelper } from './approval-gate-permission.helper';
import { ApprovalGateService } from './approval-gate.service';
import { ApprovalDecision } from './approval-gate.models';

describe('ApprovalGatePage', () => {
  const decision: ApprovalDecision = {
    id: 'dec1',
    taskId: '11111111-1111-1111-1111-111111111025',
    projectId: '55555555-5555-5555-5555-555555555501',
    operationId: 'op1',
    operationStatus: 'WAITING_FOR_HUMAN',
    decision: 'REQUIRES_HUMAN_APPROVAL',
    eligibleForMerge: false,
    stale: false,
    policyId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001',
    policyName: 'Default Approval Policy',
    policyVersion: 1,
    evidenceFingerprint: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
    decisionFingerprint: 'fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321',
    patchResultId: 'patch-11111111-1111-1111-1111-111111111001',
    patchHash: 'patchhash1111111111111111111111111111111111111111111111111111111111111111',
    gitOperationId: 'git-11111111-1111-1111-1111-111111111001',
    commitHash: 'commit1111111111111111111111111111111111111111111111111111111111111111',
    pullRequestOperationId: 'pr-11111111-1111-1111-1111-111111111001',
    pullRequestNumber: 42,
    pullRequestUrl: 'https://github.com/nova-org/nova-platform/pull/42',
    ciObservationOperationId: 'ci-11111111-1111-1111-1111-111111111001',
    ciOverallStatus: 'SUCCESS',
    ciCommitHash: 'commit1111111111111111111111111111111111111111111111111111111111111111',
    repairOperationId: null,
    reviewSummary: {
      approved: true,
      score: 85,
      criticalFindings: 0,
      highFindings: 0,
    },
    testingSummary: {
      validated: true,
      coverageEstimate: 72,
      summary: 'All planned suites covered',
    },
    repairSummary: null,
    requiredHumanApprovals: 1,
    receivedHumanApprovals: 0,
    rejectionCount: 0,
    reasonSummary: 'Automated requirements passed; awaiting human approval',
    validUntil: '2026-07-20T00:00:00Z',
    requirements: [
      {
        id: 'req1',
        ruleCode: 'CI_MUST_SUCCEED',
        description: 'CI must succeed',
        expectedValue: 'SUCCESS',
        actualValue: 'SUCCESS',
        result: 'PASSED',
        blocking: true,
        severity: 'CRITICAL',
        failureReason: null,
        evaluatedAt: '2026-07-19T00:00:00Z',
      },
      {
        id: 'req2',
        ruleCode: 'REQUIRED_HUMAN_APPROVALS',
        description: 'Human approvals required',
        expectedValue: '1',
        actualValue: '0',
        result: 'PENDING',
        blocking: true,
        severity: 'HIGH',
        failureReason: null,
        evaluatedAt: '2026-07-19T00:00:00Z',
      },
    ],
    evidence: [],
    humanActions: [],
    timeline: [
      {
        eventType: 'AUTOMATED_GATE_PASSED',
        detail: 'All blocking automated rules passed',
        actorUserId: null,
        createdAt: '2026-07-19T00:00:00Z',
      },
    ],
    errorCode: null,
    errorMessage: null,
    approvedAt: null,
    rejectedAt: null,
    supersededAt: null,
    invalidatedAt: null,
    createdAt: '2026-07-19T00:00:00Z',
    updatedAt: '2026-07-19T00:00:00Z',
  };

  const approvedDecision: ApprovalDecision = {
    ...decision,
    id: 'dec2',
    decision: 'APPROVED',
    eligibleForMerge: true,
    receivedHumanApprovals: 1,
    operationStatus: 'SUCCEEDED',
    humanActions: [
      {
        id: 'act1',
        actorUserId: '44444444-4444-4444-4444-444444444401',
        actorDisplayName: 'Nova Admin',
        action: 'APPROVE',
        commentText: 'Looks good',
        evidenceFingerprint: decision.evidenceFingerprint,
        createdAt: '2026-07-19T00:05:00Z',
      },
    ],
  };

  const staleDecision: ApprovalDecision = {
    ...decision,
    id: 'dec3',
    decision: 'SUPERSEDED',
    stale: true,
    supersededAt: '2026-07-19T01:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApprovalGatePage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: [
                'APPROVAL_GATE_RUN',
                'APPROVAL_GATE_READ',
                'APPROVAL_GATE_APPROVE',
                'APPROVAL_GATE_REJECT',
              ],
            }),
          },
        },
        {
          provide: ApprovalGateService,
          useValue: {
            run: () => of(decision),
            getLatest: () => of(decision),
            getHistory: () => of([decision]),
            getRequirements: () => of(decision.requirements),
            approve: () => of(approvedDecision),
            reject: () => of({ ...decision, decision: 'REJECTED', rejectionCount: 1 }),
          },
        },
        ApprovalGatePermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders approval gate page with safety statement', () => {
    const fixture = TestBed.createComponent(ApprovalGatePage);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Approval Gate');
    expect(text).toContain(
      'Approval Gate evaluates and records approval eligibility. It never merges or deploys code.',
    );
  });

  it('evaluates and shows decision badges and policy version', () => {
    const fixture = TestBed.createComponent(ApprovalGatePage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111025' });
    component.evaluate();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('REQUIRES_HUMAN_APPROVAL');
    expect(text).toContain('Not eligible for merge');
    expect(text).toContain('Default Approval Policy');
    expect(text).toContain('v1');
    expect(text.toLowerCase()).not.toContain('token');
    expect(text.toLowerCase()).not.toContain('secret');
  });

  it('shows masked fingerprint and requirement sections', () => {
    const fixture = TestBed.createComponent(ApprovalGatePage);
    fixture.componentInstance.result.set(decision);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('abcdef12…7890');
    expect(text).toContain('Passed');
    expect(text).toContain('Pending');
    expect(text).toContain('CI_MUST_SUCCEED');
  });

  it('shows evidence summaries and safe PR external link', () => {
    const fixture = TestBed.createComponent(ApprovalGatePage);
    fixture.componentInstance.result.set(decision);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a.external-link') as HTMLAnchorElement;
    expect(link).toBeTruthy();
    expect(link.getAttribute('href')).toBe('https://github.com/nova-org/nova-platform/pull/42');
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('#42');
    expect(text).toContain('SUCCESS');
  });

  it('shows merge-not-performed notice for APPROVED', () => {
    const fixture = TestBed.createComponent(ApprovalGatePage);
    fixture.componentInstance.result.set(approvedDecision);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Eligible for merge');
    expect(text).toContain('Merge has not been performed.');
  });

  it('disables human actions for stale decisions', () => {
    const fixture = TestBed.createComponent(ApprovalGatePage);
    const component = fixture.componentInstance;
    component.result.set(staleDecision);
    fixture.detectChanges();
    expect(component.canPerformHumanAction()).toBeFalse();
    expect(component.actionsDisabled(staleDecision)).toBeTrue();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('stale or no longer valid');
  });

  it('requires comment for rejection', () => {
    const fixture = TestBed.createComponent(ApprovalGatePage);
    const component = fixture.componentInstance;
    component.result.set(decision);
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111025' });
    fixture.detectChanges();
    component.reject();
    expect(component.error()).toContain('Rejection comment is required');
  });

  it('denies access without APPROVAL_GATE permissions', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [ApprovalGatePage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: [] }),
          },
        },
        ApprovalGatePermissionHelper,
        {
          provide: ApprovalGateService,
          useValue: {
            run: () => of(decision),
            getLatest: () => of(decision),
            getHistory: () => of([]),
            getRequirements: () => of([]),
            approve: () => of(decision),
            reject: () => of(decision),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(ApprovalGatePage);
    fixture.detectChanges();
    expect(fixture.componentInstance.unauthorized()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('APPROVAL_GATE_RUN');
  });
});
