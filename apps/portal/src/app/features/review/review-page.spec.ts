import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { ReviewPage } from './review-page';
import { ReviewPermissionHelper } from './review-permission.helper';
import { ReviewService } from './review.service';
import { ReviewResult } from './review.models';

describe('ReviewPage', () => {
  const review: ReviewResult = {
    id: 'r1',
    taskId: 't1',
    runId: 'run1',
    projectId: 'p1',
    summary: 'Overall code quality is good.',
    score: 92,
    approved: true,
    findings: [
      {
        id: 'f1',
        severity: 'MEDIUM',
        category: 'SECURITY',
        title: 'Input validation',
        description: 'Request payload is not validated.',
        recommendation: 'Add Bean Validation.',
        artifactId: 'a1',
        artifactPath: 'src/LoginService.java',
      },
      {
        id: 'f2',
        severity: 'LOW',
        category: 'NAMING',
        title: 'Rename helper',
        description: 'Helper name is unclear.',
        recommendation: 'Use a domain term.',
        artifactId: null,
        artifactPath: null,
      },
    ],
    reviewedArtifacts: [
      {
        artifactId: 'a1',
        path: 'src/LoginService.java',
        filename: 'LoginService.java',
        language: 'JAVA',
        sha256: 'abc',
      },
    ],
    severityCounts: { INFO: 0, LOW: 1, MEDIUM: 1, HIGH: 0, CRITICAL: 0 },
    tokensUsed: 20,
    model: 'review-local',
    provider: 'LOCAL',
    reviewTimeMs: 12,
    createdAt: '2026-07-18T00:00:00Z',
    validated: true,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['REVIEW_RUN', 'REVIEW_READ'],
            }),
          },
        },
        {
          provide: ReviewService,
          useValue: {
            run: () => of(review),
            getLatest: () => of(review),
          },
        },
        ReviewPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders review agent page', () => {
    const fixture = TestBed.createComponent(ReviewPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Review Agent');
  });

  it('runs review and shows score badge', () => {
    const fixture = TestBed.createComponent(ReviewPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111111' });
    component.runReview();
    fixture.detectChanges();
    expect(component.result()?.score).toBe(92);
    expect(fixture.nativeElement.textContent).toContain('Approved');
    expect(fixture.nativeElement.textContent).toContain('92');
  });

  it('filters findings by severity', () => {
    const fixture = TestBed.createComponent(ReviewPage);
    const component = fixture.componentInstance;
    component.result.set(review);
    component.onSeverityFilter('MEDIUM');
    expect(component.filteredFindings().map((f) => f.title)).toEqual(['Input validation']);
  });
});
