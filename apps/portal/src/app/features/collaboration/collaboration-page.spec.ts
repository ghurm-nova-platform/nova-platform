import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { CollaborationPage, sortTimelineEvents } from './collaboration-page';
import { CollaborationService } from './collaboration.service';
import { SessionDetail, SessionSummary, TimelineEventView } from './collaboration.models';

describe('CollaborationComponentTest', () => {
  let fixture: ComponentFixture<CollaborationPage>;

  const session: SessionSummary = {
    id: 'session-1',
    organizationId: 'org-1',
    projectId: 'proj-1',
    orchestrationRunId: null,
    name: 'Release collaboration',
    status: 'ACTIVE',
    conflictDetected: false,
    startedAt: new Date().toISOString(),
    completedAt: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const detail: SessionDetail = {
    ...session,
    sharedContext: null,
    parallelGroup: null,
    conflictDetails: null,
    createdBy: 'user-1',
    participants: [
      {
        id: 'participant-1',
        participantRole: 'PLANNER',
        status: 'ACTIVE',
        currentTaskId: 'task-1',
        progressPercent: 50,
        parallelGroup: null,
        startedAt: new Date().toISOString(),
        completedAt: null,
        createdAt: new Date().toISOString(),
      },
    ],
    tasks: [
      {
        id: 'task-1',
        taskKey: 'plan',
        title: 'Plan release',
        status: 'IN_PROGRESS',
        participantId: 'participant-1',
        dependsOnTaskId: null,
        blockedByTaskId: null,
        completedByParticipantId: null,
        artifactRef: null,
        parallelGroup: null,
        assignedAt: new Date().toISOString(),
        startedAt: new Date().toISOString(),
        completedAt: null,
        createdAt: new Date().toISOString(),
      },
    ],
    messages: [
      {
        id: 'message-1',
        senderRole: 'PLANNER',
        messageType: 'INFO',
        content: 'Planning started',
        taskId: 'task-1',
        createdAt: new Date().toISOString(),
      },
    ],
    decisions: [],
    timeline: [
      {
        id: 'event-1',
        eventType: 'STARTED',
        summary: 'Planner joined',
        actorRole: 'PLANNER',
        taskId: null,
        messageId: null,
        decisionId: null,
        details: null,
        createdAt: '2026-01-02T10:00:00.000Z',
      },
      {
        id: 'event-2',
        eventType: 'TASK_ASSIGNED',
        summary: 'Task assigned',
        actorRole: 'PLANNER',
        taskId: 'task-1',
        messageId: null,
        decisionId: null,
        details: null,
        createdAt: '2026-01-02T10:05:00.000Z',
      },
      {
        id: 'event-3',
        eventType: 'COMPLETED',
        summary: 'Session completed',
        actorRole: null,
        taskId: null,
        messageId: null,
        decisionId: null,
        details: null,
        createdAt: '2026-01-02T10:30:00.000Z',
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CollaborationPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({
              roles: ['ORG_ADMIN'],
              permissions: ['COLLABORATION_READ', 'COLLABORATION_WRITE', 'COLLABORATION_ADMIN'],
            }),
          },
        },
        {
          provide: CollaborationService,
          useValue: {
            getConfig: () => of({ enabled: true, pollingSeconds: 10, maxMessages: 100 }),
            list: () => of([session]),
            get: () => of(detail),
            pause: () => of(detail),
            resume: () => of(detail),
            cancel: () => of(detail),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CollaborationPage);
    fixture.detectChanges();
  });

  it('renders collaboration heading', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Multi-Agent Collaboration');
  });

  it('renders selected session and participant data', () => {
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Release collaboration');
    expect(element.textContent).toContain('1 participants');
    expect(element.textContent).toContain('1 tasks');

    const tabs = element.querySelectorAll('.mat-mdc-tab');
    (tabs[1] as HTMLElement).click();
    fixture.detectChanges();
    expect(element.textContent).toContain('PLANNER');

    (tabs[4] as HTMLElement).click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Plan release');
  });
});

describe('CollaborationTimelineComponentTest', () => {
  it('sorts timeline events chronologically', () => {
    const events: TimelineEventView[] = [
      {
        id: '2',
        eventType: 'TASK_ASSIGNED',
        summary: 'Task assigned',
        actorRole: 'PLANNER',
        taskId: 'task-1',
        messageId: null,
        decisionId: null,
        details: null,
        createdAt: '2026-01-02T10:05:00.000Z',
      },
      {
        id: '1',
        eventType: 'STARTED',
        summary: 'Agent started',
        actorRole: 'PLANNER',
        taskId: null,
        messageId: null,
        decisionId: null,
        details: null,
        createdAt: '2026-01-02T10:00:00.000Z',
      },
    ];

    const sorted = sortTimelineEvents(events);
    expect(sorted[0].eventType).toBe('STARTED');
    expect(sorted[1].eventType).toBe('TASK_ASSIGNED');
  });

  it('maps timeline steps for vertical flow labels', () => {
    TestBed.configureTestingModule({
      imports: [CollaborationPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({ roles: ['ORG_ADMIN'], permissions: [] }),
          },
        },
        {
          provide: CollaborationService,
          useValue: {
            getConfig: () => of({ enabled: true, pollingSeconds: 10, maxMessages: 100 }),
            list: () => of([]),
          },
        },
      ],
    });
    const component = TestBed.createComponent(CollaborationPage).componentInstance;
    expect(component.timelineStepLabel('STARTED')).toBe('Agent');
    expect(component.timelineStepLabel('TASK_ASSIGNED')).toBe('Task');
    expect(component.timelineStepLabel('DECISION')).toBe('Decision');
    expect(component.timelineStepLabel('MESSAGE_SENT')).toBe('Message');
    expect(component.timelineStepLabel('APPROVAL')).toBe('Approval');
    expect(component.timelineStepLabel('COMPLETED')).toBe('Completion');
  });
});
