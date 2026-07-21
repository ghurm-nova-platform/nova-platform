import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { PrReviewPage, exportExtension, findingsByCategory } from './pr-review-page';
import { PrReviewService } from './pr-review.service';
import {
  FindingView,
  RecommendationView,
  ReviewRunDetail,
  ReviewRunSummary,
  RiskScoreView,
} from './pr-review.models';

describe('ReviewComponentTest', () => {
  let fixture: ComponentFixture<PrReviewPage>;

  const run: ReviewRunSummary = {
    id: 'run-1',
    organizationId: 'org-1',
    projectId: 'proj-1',
    pullRequestNumber: 42,
    pullRequestTitle: 'Add auth middleware',
    status: 'COMPLETED',
    result: 'APPROVED_WITH_SUGGESTIONS',
    overallScore: 82,
    riskScore: 28,
    architectureScore: 80,
    securityScore: 75,
    performanceScore: 88,
    qualityScore: 84,
    testingScore: 70,
    documentationScore: 65,
    summary: 'Minor security and testing gaps.',
    createdBy: 'user-1',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const finding: FindingView = {
    id: 'finding-1',
    reviewRunId: 'run-1',
    category: 'Security',
    severity: 'WARNING',
    title: 'Missing input validation',
    description: 'User input is not sanitized.',
    recommendation: 'Add validation middleware.',
    references: [],
    knowledgeDocumentIds: [],
    createdAt: new Date().toISOString(),
  };

  const recommendation: RecommendationView = {
    id: 'rec-1',
    reviewRunId: 'run-1',
    priority: 'HIGH',
    title: 'Harden auth path',
    description: 'Apply validation on all auth endpoints.',
    knowledgeDocumentIds: [],
    createdAt: new Date().toISOString(),
  };

  const detail: ReviewRunDetail = {
    ...run,
    changedFiles: ['src/auth.ts'],
    findings: [finding],
    recommendations: [recommendation],
  };

  const risk: RiskScoreView = {
    reviewRunId: 'run-1',
    overallScore: 82,
    riskScore: 28,
    result: 'APPROVED_WITH_SUGGESTIONS',
    categoryScores: {
      Security: 75,
      Architecture: 80,
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrReviewPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({
              roles: ['USER'],
              permissions: ['PR_REVIEW_READ', 'PR_REVIEW_RUN'],
            }),
          },
        },
        {
          provide: PrReviewService,
          useValue: {
            list: () => of([run]),
            history: () => of([run]),
            get: () => of(detail),
            findings: () => of([finding]),
            recommendations: () => of([recommendation]),
            riskScore: () => of(risk),
            knowledge: () => of([]),
            run: () => of(detail),
            rerun: () => of(detail),
            export: () => of(new Blob(['report'])),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PrReviewPage);
    fixture.detectChanges();
  });

  it('renders PR review heading', () => {
    expect(fixture.nativeElement.textContent).toContain('PR Review Engine');
  });

  it('renders selected review data', () => {
    expect(fixture.nativeElement.textContent).toContain('Add auth middleware');
    expect(fixture.nativeElement.textContent).toContain('Missing input validation');
  });

  it('filters findings by category', () => {
    const architectureFinding: FindingView = { ...finding, id: 'f-2', category: 'Architecture' };
    const items = findingsByCategory([finding, architectureFinding], 'Architecture');
    expect(items.length).toBe(1);
    expect(items[0].category).toBe('Architecture');
  });

  it('maps export extensions', () => {
    expect(exportExtension('markdown')).toBe('md');
    expect(exportExtension('json')).toBe('json');
    expect(exportExtension('pdf')).toBe('pdf');
  });
});
