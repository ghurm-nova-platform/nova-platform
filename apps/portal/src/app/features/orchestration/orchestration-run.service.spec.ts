import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { OrchestrationRunService } from './orchestration-run.service';

describe('OrchestrationRunService', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';
  const runId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';
  const taskId = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01';
  const predecessorTaskId = 'cccccccc-cccc-cccc-cccc-cccccccccc01';
  const successorTaskId = 'dddddddd-dddd-dddd-dddd-dddddddddd01';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), RuntimeConfigService, ApiClient],
    });
  });

  it('lists runs with filters through Platform API', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(OrchestrationRunService);

    service
      .list({
        projectId,
        status: 'DRAFT',
        executionMode: 'SEQUENTIAL',
        search: 'demo',
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      })
      .subscribe();

    const req = http.expectOne(
      (r) =>
        r.url === 'http://localhost:8080/api/orchestration-runs' &&
        r.params.get('projectId') === projectId &&
        r.params.get('status') === 'DRAFT' &&
        r.params.get('executionMode') === 'SEQUENTIAL' &&
        r.params.get('search') === 'demo' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10' &&
        r.params.get('sort') === 'createdAt,desc',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });
    http.verify();
  });

  it('creates, updates, and runs lifecycle actions through Platform API', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(OrchestrationRunService);

    service
      .create({
        projectId,
        name: 'Demo run',
        objective: 'Coordinate agents',
        executionMode: 'DEPENDENCY_GRAPH',
        failurePolicy: 'FAIL_FAST',
        maxParallelTasks: 3,
        maximumDurationMs: 3600000,
      })
      .subscribe();
    const createReq = http.expectOne('http://localhost:8080/api/orchestration-runs');
    expect(createReq.request.method).toBe('POST');
    expect(createReq.request.body.projectId).toBe(projectId);
    createReq.flush({ id: runId, status: 'DRAFT', version: 0 });

    service
      .update(runId, {
        version: 0,
        name: 'Demo run updated',
        objective: 'Coordinate agents',
        executionMode: 'DEPENDENCY_GRAPH',
        failurePolicy: 'BEST_EFFORT',
      })
      .subscribe();
    const updateReq = http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}`);
    expect(updateReq.request.method).toBe('PUT');
    updateReq.flush({ id: runId, status: 'DRAFT', version: 1 });

    service.get(runId).subscribe();
    http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}`).flush({ id: runId });

    service.ready(runId).subscribe();
    http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/ready`).flush({ id: runId, status: 'READY' });

    service.start(runId).subscribe();
    http
      .expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/start`)
      .flush({ id: runId, status: 'RUNNING' });

    service.cancel(runId, { reason: 'User cancelled' }).subscribe();
    const cancelReq = http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/cancel`);
    expect(cancelReq.request.body).toEqual({ reason: 'User cancelled' });
    cancelReq.flush({ id: runId, status: 'CANCELLED' });

    service.archive(runId).subscribe();
    http
      .expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/archive`)
      .flush({ id: runId, status: 'ARCHIVED' });

    http.verify();
  });

  it('manages tasks, dependencies, graph, and events through Platform API', () => {
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(OrchestrationRunService);

    service.createTask(runId, { taskKey: 't1', displayName: 'Task 1', taskType: 'AGENT_TURN' }).subscribe();
    const createTaskReq = http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/tasks`);
    expect(createTaskReq.request.method).toBe('POST');
    createTaskReq.flush({ id: taskId, taskKey: 't1' });

    service.listTasks(runId, { status: 'DRAFT', page: 0, size: 20 }).subscribe();
    http
      .expectOne(
        (r) =>
          r.url === `http://localhost:8080/api/orchestration-runs/${runId}/tasks` &&
          r.params.get('status') === 'DRAFT',
      )
      .flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });

    service.getTask(runId, taskId).subscribe();
    http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/tasks/${taskId}`).flush({ id: taskId });

    service
      .updateTask(runId, taskId, {
        version: 0,
        taskKey: 't1',
        displayName: 'Task 1b',
        taskType: 'AGENT_TURN',
      })
      .subscribe();
    http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/tasks/${taskId}`).flush({ id: taskId });

    service.deleteTask(runId, taskId).subscribe();
    const deleteTaskReq = http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/tasks/${taskId}`);
    expect(deleteTaskReq.request.method).toBe('DELETE');
    deleteTaskReq.flush(null);

    service.listAttempts(runId, taskId).subscribe();
    http
      .expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/tasks/${taskId}/attempts`)
      .flush([]);

    service
      .addDependency(runId, {
        predecessorTaskId,
        successorTaskId,
        dependencyType: 'SUCCESS',
      })
      .subscribe();
    const addDepReq = http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/dependencies`);
    expect(addDepReq.request.method).toBe('POST');
    addDepReq.flush({ runId, predecessorTaskId, successorTaskId, dependencyType: 'SUCCESS' });

    service.removeDependency(runId, { predecessorTaskId, successorTaskId }).subscribe();
    const removeDepReq = http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/dependencies`);
    expect(removeDepReq.request.method).toBe('DELETE');
    expect(removeDepReq.request.body).toEqual({ predecessorTaskId, successorTaskId });
    removeDepReq.flush(null);

    service.getGraph(runId).subscribe();
    http.expectOne(`http://localhost:8080/api/orchestration-runs/${runId}/graph`).flush({
      runId,
      nodes: [],
      edges: [],
    });

    service.listEvents(runId, { page: 0, size: 50, sort: 'eventSequence,desc' }).subscribe();
    http
      .expectOne(
        (r) =>
          r.url === `http://localhost:8080/api/orchestration-runs/${runId}/events` &&
          r.params.get('page') === '0',
      )
      .flush({ content: [], totalElements: 0, totalPages: 0, size: 50, number: 0 });

    http.verify();
  });
});
