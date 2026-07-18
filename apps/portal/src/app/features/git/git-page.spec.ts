import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { GitPage } from './git-page';
import { GitPermissionHelper } from './git-permission.helper';
import { GitService } from './git.service';
import { GitOperation } from './git.models';

describe('GitPage', () => {
  const result: GitOperation = {
    id: 'g1',
    taskId: '11111111-1111-1111-1111-111111111024',
    runId: 'r1',
    projectId: 'p1',
    patchResultId: 'patch1',
    status: 'SUCCEEDED',
    branchName: 'ai/task-11111111-1111-1111-1111-111111111024',
    commitHash: 'abc123def456',
    patchHash: 'a'.repeat(64),
    repositoryPath: '/tmp/repo',
    baseRef: 'main',
    errorCode: null,
    validation: { valid: true, message: 'ok' },
    applyResult: { applied: true, details: 'Patch applied' },
    branches: [],
    commits: [],
    timeline: [
      { phase: 'STARTED', at: '2026-07-18T00:00:00Z', detail: 'start' },
      { phase: 'COMMITTED', at: '2026-07-18T00:00:01Z', detail: 'commit' },
    ],
    startedAt: '2026-07-18T00:00:00Z',
    completedAt: '2026-07-18T00:00:01Z',
    createdAt: '2026-07-18T00:00:01Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GitPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['GIT_RUN', 'GIT_READ'],
            }),
          },
        },
        {
          provide: GitService,
          useValue: {
            run: () => of(result),
            getLatest: () => of(result),
          },
        },
        GitPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders git integration page', () => {
    const fixture = TestBed.createComponent(GitPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Git Integration');
  });

  it('runs git and shows branch, commit, and status', () => {
    const fixture = TestBed.createComponent(GitPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111024' });
    component.runGit();
    fixture.detectChanges();
    expect(component.result()?.status).toBe('SUCCEEDED');
    expect(fixture.nativeElement.textContent).toContain('SUCCEEDED');
    expect(fixture.nativeElement.textContent).toContain('ai/task-11111111-1111-1111-1111-111111111024');
    expect(fixture.nativeElement.textContent).toContain('abc123def456');
  });

  it('copies branch and commit', async () => {
    const fixture = TestBed.createComponent(GitPage);
    const component = fixture.componentInstance;
    component.result.set(result);
    const writeText = jasmine.createSpy('writeText').and.resolveTo(undefined);
    spyOnProperty(navigator, 'clipboard', 'get').and.returnValue({ writeText } as unknown as Clipboard);
    await component.copy(result.branchName, 'branch');
    expect(writeText).toHaveBeenCalledWith(result.branchName);
    await component.copy(result.commitHash, 'commit');
    expect(writeText).toHaveBeenCalledWith(result.commitHash!);
  });
});
