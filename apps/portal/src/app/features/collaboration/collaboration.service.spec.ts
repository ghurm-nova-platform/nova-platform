import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiClient } from '../../core/http/api-client';
import { CollaborationService } from './collaboration.service';

describe('CollaborationService', () => {
  it('wraps collaboration API endpoints', () => {
    const api = jasmine.createSpyObj<ApiClient>('ApiClient', ['get', 'post']);
    api.get.and.returnValue(of([]));
    api.post.and.returnValue(of({ id: 'session-1' }));

    TestBed.configureTestingModule({
      providers: [CollaborationService, { provide: ApiClient, useValue: api }],
    });
    const service = TestBed.inject(CollaborationService);

    service.list().subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/collaboration');

    service.list('proj-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/collaboration?projectId=proj-1');

    service.getConfig().subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/collaboration/config');

    service.get('session-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/collaboration/session-1');

    service.timeline('session-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/collaboration/session-1/timeline');

    service.participants('session-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/collaboration/session-1/participants');

    service.messages('session-1').subscribe();
    expect(api.get).toHaveBeenCalledWith('/api/collaboration/session-1/messages');

    service
      .create({ projectId: 'proj-1', name: 'Run collaboration' })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/collaboration', {
      projectId: 'proj-1',
      name: 'Run collaboration',
    });

    service
      .assign('session-1', { taskId: 'task-1', action: 'COMPLETE' })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/collaboration/session-1/assign', {
      taskId: 'task-1',
      action: 'COMPLETE',
    });

    service
      .sendMessage('session-1', {
        senderRole: 'PLANNER',
        messageType: 'INFO',
        content: 'Update',
      })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/collaboration/session-1/message', {
      senderRole: 'PLANNER',
      messageType: 'INFO',
      content: 'Update',
    });

    service
      .recordDecision('session-1', {
        decisionType: 'APPROVE',
        summary: 'Approved',
      })
      .subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/collaboration/session-1/decision', {
      decisionType: 'APPROVE',
      summary: 'Approved',
    });

    service.pause('session-1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/collaboration/session-1/pause', {});

    service.resume('session-1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/collaboration/session-1/resume', {});

    service.cancel('session-1').subscribe();
    expect(api.post).toHaveBeenCalledWith('/api/collaboration/session-1/cancel', {});
  });
});
