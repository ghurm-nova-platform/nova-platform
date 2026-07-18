import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { TestingPage } from './testing-page';
import { TestingPermissionHelper } from './testing-permission.helper';
import { TestingService } from './testing.service';
import { TestingResult } from './testing.models';

describe('TestingPage', () => {
  const result: TestingResult = {
    id: 'tr1',
    taskId: 't1',
    runId: 'r1',
    projectId: 'p1',
    summary: 'Unit and API tests generated.',
    coverageEstimate: 84,
    generatedTests: [
      {
        id: 'g1',
        type: 'UNIT',
        priority: 'HIGH',
        title: 'LoginService validation',
        description: 'Verify invalid credentials.',
        artifactId: 'a1',
        artifactPath: 'src/LoginService.java',
        cases: [
          {
            id: 'c1',
            generatedTestId: 'g1',
            name: 'rejects blank password',
            steps: 'Call validate',
            expectedResult: 'throws',
            priority: 'HIGH',
          },
        ],
      },
      {
        id: 'g2',
        type: 'API',
        priority: 'MEDIUM',
        title: 'Login endpoint',
        description: 'API contract',
        artifactId: null,
        artifactPath: null,
        cases: [],
      },
    ],
    testCases: [],
    reviewedArtifacts: [],
    typeCounts: { UNIT: 1, API: 1 },
    priorityCounts: { HIGH: 1, MEDIUM: 1, LOW: 0, CRITICAL: 0 },
    tokensUsed: 20,
    model: 'testing-local',
    provider: 'LOCAL',
    generationTimeMs: 12,
    createdAt: '2026-07-18T00:00:00Z',
    validated: true,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestingPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['TESTING_RUN', 'TESTING_READ'],
            }),
          },
        },
        {
          provide: TestingService,
          useValue: {
            run: () => of(result),
            getLatest: () => of(result),
          },
        },
        TestingPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders testing agent page', () => {
    const fixture = TestBed.createComponent(TestingPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Testing Agent');
  });

  it('runs testing and shows coverage', () => {
    const fixture = TestBed.createComponent(TestingPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111111' });
    component.runTesting();
    fixture.detectChanges();
    expect(component.result()?.coverageEstimate).toBe(84);
    expect(fixture.nativeElement.textContent).toContain('84');
    expect(fixture.nativeElement.textContent).toContain('HIGH');
  });

  it('filters tests by type', () => {
    const fixture = TestBed.createComponent(TestingPage);
    const component = fixture.componentInstance;
    component.result.set(result);
    component.onTypeFilter('UNIT');
    expect(component.filteredTests().map((t) => t.title)).toEqual(['LoginService validation']);
  });

  it('searches tests by title', () => {
    const fixture = TestBed.createComponent(TestingPage);
    const component = fixture.componentInstance;
    component.result.set(result);
    component.onSearch('endpoint');
    expect(component.filteredTests().map((t) => t.type)).toEqual(['API']);
  });
});
