import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PullRequestPage } from './pull-request-page';
import { PullRequestPermissionHelper } from './pull-request-permission.helper';
import { PullRequestService } from './pull-request.service';
import { PullRequestOperation } from './pull-request.models';

describe('PullRequestPage', () => {
  const result: PullRequestOperation = {
    id: 'pr1',
    taskId: '11111111-1111-1111-1111-111111111024',
    projectId: '55555555-5555-5555-5555-555555555501',
    gitOperationId: 'g1',
    patchResultId: 'patch1',
    status: 'SUCCEEDED',
    provider: 'LOCAL',
    repositoryOwner: 'ghurm-nova-platform',
    repositoryName: 'nova-demo',
    remoteName: 'origin',
    remoteUrl: 'memory://ghurm-nova-platform/nova-demo',
    sourceBranch: 'ai/task-11111111-1111-1111-1111-111111111024',
    targetBranch: 'main',
    localCommitHash: 'abc123def4567890123456789012345678901234567890',
    remoteCommitHash: 'abc123def4567890123456789012345678901234567890',
    patchHash: 'a'.repeat(64),
    pullRequestNumber: 1,
    pullRequestUrl: 'memory://ghurm-nova-platform/nova-demo/pull/1',
    pullRequestTitle: 'Nova PR',
    errorCode: null,
    validation: { valid: true, message: 'Pull request created' },
    remotePush: null,
    pullRequestRecord: null,
    timeline: [
      { phase: 'STARTED', at: '2026-07-18T00:00:00Z', detail: 'start' },
      { phase: 'COMPLETED', at: '2026-07-18T00:00:01Z', detail: 'SUCCEEDED' },
    ],
    startedAt: '2026-07-18T00:00:00Z',
    completedAt: '2026-07-18T00:00:01Z',
    createdAt: '2026-07-18T00:00:01Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PullRequestPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['PR_RUN', 'PR_READ'],
            }),
          },
        },
        {
          provide: PullRequestService,
          useValue: {
            run: () => of(result),
            getLatest: () => of(result),
            getHistory: () => of([result]),
          },
        },
        PullRequestPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders pull request agent page with safety statement', () => {
    const fixture = TestBed.createComponent(PullRequestPage);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Pull Request Agent');
    expect(text).toContain('This agent creates Pull Requests but never approves or merges them.');
  });

  it('runs pull request agent and shows status and metadata', () => {
    const fixture = TestBed.createComponent(PullRequestPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111024' });
    component.runPullRequest();
    fixture.detectChanges();
    expect(component.result()?.status).toBe('SUCCEEDED');
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('SUCCEEDED');
    expect(text).toContain('ghurm-nova-platform/nova-demo');
    expect(text).toContain('LOCAL');
    expect(text.toLowerCase()).not.toContain('token');
  });

  it('opens pull request link in a new tab safely', () => {
    const fixture = TestBed.createComponent(PullRequestPage);
    fixture.componentInstance.result.set(result);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a.pr-link') as HTMLAnchorElement;
    expect(link).toBeTruthy();
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('shows error code separately when present', () => {
    const fixture = TestBed.createComponent(PullRequestPage);
    fixture.componentInstance.result.set({ ...result, status: 'FAILED', errorCode: 'PR_PUSH_FAILED' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PR_PUSH_FAILED');
  });

  it('copies branch and commit hashes', async () => {
    const fixture = TestBed.createComponent(PullRequestPage);
    const component = fixture.componentInstance;
    component.result.set(result);
    const writeText = jasmine.createSpy('writeText').and.resolveTo(undefined);
    spyOnProperty(navigator, 'clipboard', 'get').and.returnValue({ writeText } as unknown as Clipboard);
    await component.copy(result.sourceBranch, 'branch');
    expect(writeText).toHaveBeenCalledWith(result.sourceBranch);
    await component.copy(result.localCommitHash, 'local-commit');
    expect(writeText).toHaveBeenCalledWith(result.localCommitHash);
  });
});
