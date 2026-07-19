import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { ReleasePage } from './release-page';
import { ReleasePermissionHelper } from './release-permission.helper';
import { ReleaseService } from './release.service';
import { Release } from './release.models';

describe('ReleasePage', () => {
  const release: Release = {
    id: 'rel1',
    organizationId: '11111111-1111-1111-1111-111111111111',
    projectId: '55555555-5555-5555-5555-555555555501',
    releaseNumber: 1,
    semanticVersion: '1.2.3',
    releaseName: 'Sprint 4',
    description: 'Release manager demo',
    status: 'READY',
    versionStrategy: 'SEMVER',
    bumpType: 'PATCH',
    contentFingerprint: 'fp123',
    manifestHash: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
    manifestJson: '{"semanticVersion":"1.2.3"}',
    errorCode: null,
    errorMessage: null,
    createdBy: '44444444-4444-4444-4444-444444444401',
    contents: [
      {
        id: 'c1',
        contentType: 'PULL_REQUEST',
        referenceId: 'pr-1',
        commitSha: null,
        sortOrder: 0,
      },
      {
        id: 'c2',
        contentType: 'COMMIT',
        referenceId: null,
        commitSha: 'deadbeef',
        sortOrder: 1,
      },
    ],
    artifacts: [],
    version: {
      id: 'v1',
      semanticVersion: '1.2.3',
      versionStrategy: 'SEMVER',
      bumpType: 'PATCH',
      majorVersion: 1,
      minorVersion: 2,
      patchVersion: 3,
      createdAt: '2026-07-19T00:00:00Z',
    },
    timeline: [
      { eventType: 'CREATED', at: '2026-07-19T00:00:00Z', detail: 'created' },
      { eventType: 'READY', at: '2026-07-19T00:01:00Z', detail: 'ready' },
    ],
    preparedAt: '2026-07-19T00:01:00Z',
    publishedAt: null,
    archivedAt: null,
    createdAt: '2026-07-19T00:00:00Z',
    updatedAt: '2026-07-19T00:01:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleasePage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['RELEASE_RUN', 'RELEASE_READ'],
            }),
          },
        },
        {
          provide: ReleaseService,
          useValue: {
            create: () => of(release),
            prepare: () => of(release),
            publish: () => of({ ...release, status: 'PUBLISHED' }),
            list: () => of([release]),
            get: () => of(release),
            history: () => of(release),
          },
        },
        ReleasePermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders release manager heading and safety statement', () => {
    const fixture = TestBed.createComponent(ReleasePage);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Release Manager');
    expect(text).toContain('does not deploy');
  });

  it('shows version, PRs, commits, manifest hash, and timeline after selection', () => {
    const fixture = TestBed.createComponent(ReleasePage);
    const page = fixture.componentInstance;
    page.selected.set(release);
    page.releases.set([release]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('1.2.3');
    expect(text).toContain('deadbeef');
    expect(text).toContain('pr-1');
    expect(text).toContain(release.manifestHash!);
    expect(text).toContain('CREATED');
    expect(text).toContain('READY');
    expect(text).toContain('History');
  });
});
