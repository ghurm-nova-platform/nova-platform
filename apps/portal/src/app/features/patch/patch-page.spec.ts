import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PatchPage } from './patch-page';
import { PatchPermissionHelper } from './patch-permission.helper';
import { PatchService } from './patch.service';
import { PatchResult } from './patch.models';

describe('PatchPage', () => {
  const result: PatchResult = {
    id: 'p1',
    taskId: 't1',
    runId: 'r1',
    projectId: 'proj1',
    summary: 'Generated patch',
    status: 'VALID',
    statistics: { filesChanged: 1, insertions: 1, deletions: 0, patchSize: 120 },
    patch: '--- a/src/A.java\n+++ b/src/A.java\n@@ -1,1 +1,2 @@\n class A {}\n+// ok\n',
    files: [
      {
        id: 'f1',
        path: 'src/A.java',
        oldPath: 'src/A.java',
        newPath: 'src/A.java',
        changeType: 'MODIFY',
        insertions: 1,
        deletions: 0,
        patchExcerpt: '--- a/src/A.java\n+++ b/src/A.java',
      },
    ],
    artifacts: [],
    validation: { valid: true, message: 'Unified diff validated' },
    tokensUsed: 20,
    model: 'patch-local',
    provider: 'LOCAL',
    generationTimeMs: 12,
    createdAt: '2026-07-18T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatchPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['PATCH_RUN', 'PATCH_READ'],
            }),
          },
        },
        {
          provide: PatchService,
          useValue: {
            run: () => of(result),
            getLatest: () => of(result),
          },
        },
        PatchPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders patch agent page', () => {
    const fixture = TestBed.createComponent(PatchPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Patch Agent');
  });

  it('runs patch and shows validation badge and statistics', () => {
    const fixture = TestBed.createComponent(PatchPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111111' });
    component.runPatch();
    fixture.detectChanges();
    expect(component.result()?.status).toBe('VALID');
    expect(fixture.nativeElement.textContent).toContain('VALID');
    expect(fixture.nativeElement.textContent).toContain('Files: 1');
    expect(fixture.nativeElement.textContent).toContain('+1');
  });

  it('shows unified diff viewer', () => {
    const fixture = TestBed.createComponent(PatchPage);
    const component = fixture.componentInstance;
    component.result.set(result);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unified diff');
    expect(fixture.nativeElement.textContent).toContain('src/A.java');
  });

  it('downloads patch file', () => {
    const fixture = TestBed.createComponent(PatchPage);
    const component = fixture.componentInstance;
    component.result.set(result);
    const createObjectURL = spyOn(URL, 'createObjectURL').and.returnValue('blob:patch');
    const revokeObjectURL = spyOn(URL, 'revokeObjectURL');
    const click = jasmine.createSpy('click');
    spyOn(document, 'createElement').and.returnValue({
      href: '',
      download: '',
      click,
    } as unknown as HTMLAnchorElement);
    component.downloadPatch();
    expect(createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalled();
  });
});
