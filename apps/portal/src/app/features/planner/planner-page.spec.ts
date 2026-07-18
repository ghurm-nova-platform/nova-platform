import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { ProjectService } from '../projects/project.service';
import { PlannerPage } from './planner-page';
import { PlannerPermissionHelper } from './planner-permission.helper';
import { PlannerService } from './planner.service';

describe('PlannerPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlannerPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['PLANNER_PLAN', 'PLANNER_IMPORT', 'PLANNER_TEMPLATE_READ'],
            }),
          },
        },
        {
          provide: ProjectService,
          useValue: {
            list: () => of({ content: [{ id: 'p1', name: 'Demo' }], totalElements: 1 }),
          },
        },
        {
          provide: PlannerService,
          useValue: {
            listTemplates: () => of([]),
            plan: () =>
              of({
                validated: true,
                estimate: {
                  complexity: 'MEDIUM',
                  riskLevel: 'MEDIUM',
                  estimatedTokens: 1000,
                  estimatedDurationSeconds: 60,
                  estimatedCostUsd: 0.01,
                },
                plan: {
                  objective: 'Build auth',
                  executionMode: 'DEPENDENCY_GRAPH',
                  failurePolicy: 'FAIL_FAST',
                  tasks: [
                    {
                      taskKey: 'analysis',
                      displayName: 'Analyze',
                      taskType: 'AGENT_TURN',
                      agentRole: 'research',
                      priority: 1,
                    },
                  ],
                  dependencies: [],
                },
              }),
            importPlan: () => of({ id: 'run-1' }),
          },
        },
        PlannerPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders planner form', () => {
    const fixture = TestBed.createComponent(PlannerPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('AI Planner');
  });

  it('generates and previews a plan', () => {
    const fixture = TestBed.createComponent(PlannerPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({
      projectId: 'p1',
      objective: 'Build auth',
      runName: '',
      templateId: '',
      metadataJson: '',
    });
    component.generatePlan();
    fixture.detectChanges();
    expect(component.plannerResult()?.plan.tasks.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('DAG preview');
  });
});
