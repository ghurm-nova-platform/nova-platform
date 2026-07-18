import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { CodingPage } from './coding-page';
import { CodingPermissionHelper } from './coding-permission.helper';
import { CodingService } from './coding.service';
import { GeneratedArtifact } from './coding.models';

describe('CodingPage', () => {
  const artifact: GeneratedArtifact = {
    id: 'a1',
    organizationId: 'o1',
    projectId: 'p1',
    runId: 'r1',
    taskId: 't1',
    artifactType: 'SOURCE_FILE',
    language: 'JAVA',
    path: 'src/LoginService.java',
    filename: 'LoginService.java',
    content: 'class LoginService {}',
    sha256: 'abc123def456',
    tokensUsed: 30,
    model: 'coding-local',
    provider: 'LOCAL',
    generationTimeMs: 12,
    createdAt: '2026-07-18T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CodingPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['CODING_GENERATE', 'CODING_READ'],
            }),
          },
        },
        {
          provide: CodingService,
          useValue: {
            generate: () =>
              of({
                taskId: 't1',
                runId: 'r1',
                projectId: 'p1',
                summary: 'Implemented login page',
                artifacts: [artifact],
                tokensUsed: 30,
                model: 'coding-local',
                provider: 'LOCAL',
                generationTimeMs: 12,
                validated: true,
              }),
            listArtifacts: () => of([artifact]),
          },
        },
        CodingPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders coding agent page', () => {
    const fixture = TestBed.createComponent(CodingPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Coding Agent');
  });

  it('generates and previews artifacts', () => {
    const fixture = TestBed.createComponent(CodingPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111111' });
    component.generate();
    fixture.detectChanges();
    expect(component.artifacts().length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('LoginService.java');
    expect(fixture.nativeElement.textContent).toContain('Implemented login page');
  });

  it('filters artifacts by search', () => {
    const fixture = TestBed.createComponent(CodingPage);
    const component = fixture.componentInstance;
    component.artifacts.set([
      artifact,
      { ...artifact, id: 'a2', filename: 'Other.ts', path: 'src/Other.ts', language: 'TYPESCRIPT' },
    ]);
    component.onSearch('login');
    expect(component.filteredArtifacts().map((a) => a.filename)).toEqual(['LoginService.java']);
  });

  it('downloads selected artifact', () => {
    const fixture = TestBed.createComponent(CodingPage);
    const component = fixture.componentInstance;
    component.selected.set(artifact);
    const createObjectURL = spyOn(URL, 'createObjectURL').and.returnValue('blob:mock');
    const revoke = spyOn(URL, 'revokeObjectURL');
    const click = jasmine.createSpy('click');
    spyOn(document, 'createElement').and.returnValue({ click, download: '', href: '' } as unknown as HTMLAnchorElement);
    component.downloadSelected();
    expect(createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(revoke).toHaveBeenCalled();
  });
});
