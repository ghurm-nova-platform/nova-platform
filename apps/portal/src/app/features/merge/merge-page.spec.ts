import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { MergePage } from './merge-page';
import { MergePermissionHelper } from './merge-permission.helper';
import { MergeService } from './merge.service';
import { MergeOperation } from './merge.models';

describe('MergePage', () => {
  const operation: MergeOperation = {
    id: 'merge1',
    taskId: '11111111-1111-1111-1111-111111111025',
    projectId: '55555555-5555-5555-5555-555555555501',
    status: 'SUCCEEDED',
    mergeMethod: 'SQUASH',
    approvalDecisionId: 'dec-11111111-1111-1111-1111-111111111001',
    eligibleForMerge: true,
    evidenceFingerprint: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
    decisionFingerprint: 'fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321',
    pullRequestNumber: 42,
    repositoryOwner: 'nova-org',
    repositoryName: 'nova-platform',
    validations: [
      {
        id: 'val1',
        checkCode: 'APPROVAL_DECISION_APPROVED',
        expectedValue: 'APPROVED',
        actualValue: 'APPROVED',
        result: 'PASSED',
        failureReason: null,
        evaluatedAt: '2026-07-19T00:00:00Z',
      },
      {
        id: 'val2',
        checkCode: 'EVIDENCE_FINGERPRINT_MATCH',
        expectedValue: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
        actualValue: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
        result: 'PASSED',
        failureReason: null,
        evaluatedAt: '2026-07-19T00:00:01Z',
      },
    ],
    mergedCommit: {
      hash: 'merged1111111111111111111111111111111111111111111111111111111111111111',
      mergeMethod: 'SQUASH',
      pullRequestNumber: 42,
      pullRequestUrl: 'https://github.com/nova-org/nova-platform/pull/42',
      mergedAt: '2026-07-19T00:01:00Z',
      provider: 'GITHUB',
    },
    timeline: [
      {
        eventType: 'VALIDATION_PASSED',
        detail: 'Approval Gate decision validated',
        createdAt: '2026-07-19T00:00:05Z',
      },
      {
        eventType: 'MERGE_SUCCEEDED',
        detail: 'Pull request merged via provider',
        createdAt: '2026-07-19T00:01:00Z',
      },
    ],
    errorCode: null,
    errorMessage: null,
    startedAt: '2026-07-19T00:00:00Z',
    completedAt: '2026-07-19T00:01:00Z',
    createdAt: '2026-07-19T00:01:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MergePage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['MERGE_RUN', 'MERGE_READ'],
            }),
          },
        },
        {
          provide: MergeService,
          useValue: {
            run: () => of(operation),
            getLatest: () => of(operation),
            getHistory: () => of([operation]),
          },
        },
        MergePermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders merge agent page with safety statement', () => {
    const fixture = TestBed.createComponent(MergePage);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Merge Agent');
    expect(text).toContain(
      'Merge Agent performs repository merge only after successful Approval Gate validation.',
    );
  });

  it('runs merge agent and shows status, approval decision, and merged commit', () => {
    const fixture = TestBed.createComponent(MergePage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111025' });
    component.runMerge();
    fixture.detectChanges();
    expect(component.result()?.status).toBe('SUCCEEDED');
    expect(component.result()?.approvalDecisionId).toBeTruthy();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('SUCCEEDED');
    expect(text).toContain('SQUASH');
    expect(text).toContain('Validation summary');
    expect(text).toContain('Passed');
    expect(text).toContain('merged1111111111111111111111111111111111111111111111111111111111111111');
    expect(text.toLowerCase()).not.toContain('token');
    expect(text.toLowerCase()).not.toContain('secret');
  });

  it('shows timeline events', () => {
    const fixture = TestBed.createComponent(MergePage);
    fixture.componentInstance.result.set(operation);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Timeline');
    expect(text).toContain('MERGE_SUCCEEDED');
  });

  it('denies access without MERGE permissions', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [MergePage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: [] }),
          },
        },
        MergePermissionHelper,
        {
          provide: MergeService,
          useValue: {
            run: () => of(operation),
            getLatest: () => of(operation),
            getHistory: () => of([]),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MergePage);
    fixture.detectChanges();
    expect(fixture.componentInstance.unauthorized()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('MERGE_RUN or MERGE_READ');
  });
});
