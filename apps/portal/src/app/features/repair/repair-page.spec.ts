import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { RepairPage } from './repair-page';
import { RepairPermissionHelper } from './repair-permission.helper';
import { RepairService } from './repair.service';
import { RepairOperation } from './repair.models';

describe('RepairPage', () => {
  const result: RepairOperation = {
    id: 'rep1',
    taskId: '11111111-1111-1111-1111-111111111025',
    projectId: '55555555-5555-5555-5555-555555555501',
    status: 'SUCCEEDED',
    attemptNumber: 2,
    priorPatchResultId: 'patch-prior-11111111-1111-1111-1111-111111111001',
    patchResultId: 'patch-new-11111111-1111-1111-1111-111111111002',
    reason: 'CI unit tests failed after prior patch',
    summary: 'Fix null pointer in FooService and update test fixture.',
    confidence: 0.82,
    repairedFiles: [
      {
        path: 'src/main/java/FooService.java',
        changeType: 'MODIFY',
        summary: 'Guard against null repository response',
      },
      {
        path: 'src/test/java/FooServiceTest.java',
        changeType: 'MODIFY',
        summary: 'Stub repository for edge case',
      },
    ],
    inputs: [
      {
        id: 'in1',
        sourceType: 'CI',
        sourceRef: 'build / Run tests',
        priority: 1,
        detail: 'NullPointerException at FooService.java:42',
      },
      {
        id: 'in2',
        sourceType: 'REVIEW',
        sourceRef: 'review-finding-3',
        priority: 2,
        detail: 'Missing null check on repository lookup',
      },
    ],
    actions: [
      {
        id: 'act1',
        actionType: 'ADD_NULL_CHECK',
        targetPath: 'src/main/java/FooService.java',
        description: 'Return empty optional when repository returns null',
      },
    ],
    timeline: [
      {
        phase: 'COLLECTING',
        at: '2026-07-19T00:00:00Z',
        detail: 'Loaded CI observation and review findings',
      },
      {
        phase: 'GENERATING_PATCH',
        at: '2026-07-19T00:00:30Z',
        detail: 'Invoked model gateway for repair patch',
      },
      {
        phase: 'SUCCEEDED',
        at: '2026-07-19T00:01:05Z',
        detail: 'New PatchResult persisted',
      },
    ],
    errorCode: null,
    errorMessage: null,
    startedAt: '2026-07-19T00:00:00Z',
    completedAt: '2026-07-19T00:01:05Z',
    createdAt: '2026-07-19T00:01:05Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RepairPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['ORG_ADMIN'],
              permissions: ['REPAIR_RUN', 'REPAIR_READ'],
            }),
          },
        },
        {
          provide: RepairService,
          useValue: {
            run: () => of(result),
            getLatest: () => of(result),
            getHistory: () => of([result]),
          },
        },
        RepairPermissionHelper,
      ],
    }).compileComponents();
  });

  it('renders repair agent page with safety statement', () => {
    const fixture = TestBed.createComponent(RepairPage);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Repair Agent');
    expect(text).toContain('Repair Agent proposes fixes only. It never merges code.');
  });

  it('runs repair agent and shows status, reason, and metadata', () => {
    const fixture = TestBed.createComponent(RepairPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.setValue({ taskId: '11111111-1111-1111-1111-111111111025' });
    component.runRepair();
    fixture.detectChanges();
    expect(component.result()?.status).toBe('SUCCEEDED');
    expect(component.result()?.patchResultId).toBeTruthy();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('SUCCEEDED');
    expect(text).toContain('CI unit tests failed after prior patch');
    expect(text).toContain('Failure inputs');
    expect(text).toContain('Affected files');
    expect(text).toContain('FooService.java');
    expect(text).toContain('82%');
    expect(text.toLowerCase()).not.toContain('token');
    expect(text.toLowerCase()).not.toContain('secret');
  });

  it('shows timeline events', () => {
    const fixture = TestBed.createComponent(RepairPage);
    fixture.componentInstance.result.set(result);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Timeline');
    expect(text).toContain('GENERATING_PATCH');
  });

  it('denies access without REPAIR permissions', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [RepairPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['USER'], permissions: [] }),
          },
        },
        RepairPermissionHelper,
        {
          provide: RepairService,
          useValue: { run: () => of(result), getLatest: () => of(result), getHistory: () => of([]) },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(RepairPage);
    fixture.detectChanges();
    expect(fixture.componentInstance.unauthorized()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('REPAIR_RUN or REPAIR_READ');
  });
});
