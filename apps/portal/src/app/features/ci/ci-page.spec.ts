import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { CiPage } from './ci-page';
import { CiPermissionHelper } from './ci-permission.helper';
import { CiService } from './ci.service';
import { CiObservationOperation } from './ci.models';

describe('CiPage', () => {
  const result: CiObservationOperation = {
    id: 'ci1',
    taskId: '11111111-1111-1111-1111-111111111024',
    projectId: '55555555-5555-5555-5555-555555555501',
    pullRequestOperationId: 'pr1',
    status: 'SUCCEEDED',
    provider: 'GITHUB',
    repositoryOwner: 'ghurm-nova-platform',
    repositoryName: 'nova-demo',
    sourceBranch: 'ai/task-11111111-1111-1111-1111-111111111024',
    targetBranch: 'main',
    commitHash: 'abc123def4567890123456789012345678901234567890',
    pullRequestNumber: 42,
    overallStatus: 'FAILED',
    failureSummary: 'Unit tests failed in build job.',
    retryRecommendation: 'Fix failing test in src/app/foo.spec.ts and push again.',
    errorCode: null,
    errorMessage: null,
    workflowRuns: [
      {
        id: 'wf1',
        externalWorkflowId: '12345',
        workflowName: 'CI',
        externalRunId: '987654321',
        runUrl: 'https://github.com/ghurm-nova-platform/nova-demo/actions/runs/987654321',
        status: 'completed',
        conclusion: 'failure',
        durationMs: 125000,
        triggerEvent: 'pull_request',
        commitHash: 'abc123def4567890123456789012345678901234567890',
        branch: 'ai/task-11111111-1111-1111-1111-111111111024',
        pullRequestNumber: 42,
        failureReason: 'Job build failed',
        startedAt: '2026-07-18T00:00:00Z',
        completedAt: '2026-07-18T00:02:05Z',
        jobs: [
          {
            id: 'job1',
            externalJobId: '111',
            jobName: 'build',
            status: 'completed',
            conclusion: 'failure',
            durationMs: 90000,
            failureReason: 'Process completed with exit code 1',
            startedAt: '2026-07-18T00:00:10Z',
            completedAt: '2026-07-18T00:01:40Z',
            steps: [
              {
                id: 'step1',
                stepNumber: 1,
                stepName: 'Checkout',
                status: 'completed',
                conclusion: 'success',
                durationMs: 5000,
                failureReason: null,
                startedAt: '2026-07-18T00:00:10Z',
                completedAt: '2026-07-18T00:00:15Z',
              },
              {
                id: 'step2',
                stepNumber: 2,
                stepName: 'Run tests',
                status: 'completed',
                conclusion: 'failure',
                durationMs: 80000,
                failureReason: 'Tests failed',
                startedAt: '2026-07-18T00:00:20Z',
                completedAt: '2026-07-18T00:01:40Z',
              },
            ],
          },
        ],
      },
    ],
    startedAt: '2026-07-18T00:00:00Z',
    completedAt: '2026-07-18T00:02:05Z',
    createdAt: '2026-07-18T00:02:05Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CiPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['CI_RUN', 'CI_READ'],
            }),
          },
        },
        {
          provide: CiService,
          useValue: {
            run: () => of(result),
            getLatest: () => of(result),
            getHistory: () => of([result]),
          },
        },
        CiPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders CI observation agent page with safety statement', () => {
    const fixture = TestBed.createComponent(CiPage);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('CI Observation Agent');
    expect(text).toContain(
      'This agent observes CI only. It never reruns, approves, merges, or deploys.',
    );
  });

  it('runs CI observation agent and shows status and metadata', () => {
    const fixture = TestBed.createComponent(CiPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111024' });
    component.runObservation();
    fixture.detectChanges();
    expect(component.result()?.status).toBe('SUCCEEDED');
    expect(component.result()?.overallStatus).toBe('FAILED');
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('SUCCEEDED');
    expect(text).toContain('FAILED');
    expect(text).toContain('ghurm-nova-platform/nova-demo');
    expect(text).toContain('GITHUB');
    expect(text).toContain('Failure summary');
    expect(text).toContain('Retry recommendation');
    expect(text.toLowerCase()).not.toContain('token');
    expect(text.toLowerCase()).not.toContain('credential');
  });

  it('opens GitHub Actions link in a new tab safely', () => {
    const fixture = TestBed.createComponent(CiPage);
    fixture.componentInstance.result.set(result);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a.actions-link') as HTMLAnchorElement;
    expect(link).toBeTruthy();
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('shows failed jobs and steps', () => {
    const fixture = TestBed.createComponent(CiPage);
    fixture.componentInstance.result.set(result);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Failed jobs');
    expect(text).toContain('Failed steps');
    expect(text).toContain('Run tests');
  });

  it('denies access without CI permissions', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CiPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: [] }),
          },
        },
        CiPermissionHelper,
        {
          provide: CiService,
          useValue: { run: () => of(result), getLatest: () => of(result), getHistory: () => of([]) },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(CiPage);
    fixture.detectChanges();
    expect(fixture.componentInstance.unauthorized()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('CI_RUN or CI_READ');
  });
});
