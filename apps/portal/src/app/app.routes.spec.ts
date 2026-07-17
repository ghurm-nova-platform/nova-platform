import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { routes } from './app.routes';
import { App } from './app';
import { UserSessionService } from './auth/services/user-session.service';
import { AuthUser } from './auth/services/auth.models';

describe('App routing', () => {
  const demoUser: AuthUser = {
    userId: '44444444-4444-4444-4444-444444444401',
    organizationId: '11111111-1111-1111-1111-111111111111',
    email: 'admin@nova.local',
    displayName: 'Nova Admin',
    roles: ['ORG_ADMIN'],
    permissions: [
      'AGENT_READ',
      'AGENT_CREATE',
      'AGENT_UPDATE',
      'AGENT_ACTIVATE',
      'AGENT_ARCHIVE',
      'PROMPT_READ',
      'PROMPT_CREATE',
      'PROMPT_UPDATE',
      'PROMPT_PUBLISH',
      'PROMPT_ARCHIVE',
      'PROMPT_COMPARE',
      'PROMPT_PREVIEW',
      'AGENT_EXECUTE',
      'EXECUTION_READ',
      'EXECUTION_CANCEL',
      'CONVERSATION_READ',
      'CONVERSATION_CREATE',
      'CONVERSATION_UPDATE',
      'CONVERSATION_ARCHIVE',
      'CONVERSATION_MESSAGE_READ',
      'CONVERSATION_MESSAGE_CREATE',
      'TOOL_READ',
      'TOOL_CREATE',
      'TOOL_UPDATE',
      'TOOL_ACTIVATE',
      'TOOL_ARCHIVE',
      'TOOL_ASSIGN',
      'TOOL_EXECUTE',
      'TOOL_CALL_READ',
      'TOOL_CALL_APPROVE',
    ],
  };

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideAnimationsAsync()],
    }).compileComponents();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('redirects anonymous users from root to login', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    fixture.detectChanges();
    await router.navigateByUrl('/');
    fixture.detectChanges();
    expect(router.url).toBe('/login');
  });

  it('navigates authenticated users to administration routes', async () => {
    const session = TestBed.inject(UserSessionService);
    session.setSession(
      { accessToken: 'test-access', refreshToken: 'test-refresh' },
      demoUser,
    );

    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    fixture.detectChanges();

    for (const path of [
      '/dashboard',
      '/organizations',
      '/projects',
      '/feedback',
      '/settings',
    ]) {
      await router.navigateByUrl(path);
      fixture.detectChanges();
      expect(router.url).toBe(path);
    }

    await router.navigateByUrl('/agents');
    fixture.detectChanges();
    expect(router.url).toBe('/projects');

    await router.navigateByUrl('/prompts');
    fixture.detectChanges();
    expect(router.url).toBe('/projects');

    const projectId = '55555555-5555-5555-5555-555555555501';
    const agentId = '66666666-6666-6666-6666-666666666601';
    const toolId = '77777777-7777-7777-7777-777777777701';
    const executionId = '99999999-9999-9999-9999-999999999901';

    for (const [path, expected] of [
      [`/projects/${projectId}/tools`, `/projects/${projectId}/tools`],
      [`/projects/${projectId}/tools/new`, `/projects/${projectId}/tools/new`],
      [`/projects/${projectId}/tools/${toolId}`, `/projects/${projectId}/tools/${toolId}`],
      [`/projects/${projectId}/agents/${agentId}/tools`, `/projects/${projectId}/agents/${agentId}/tools`],
      [
        `/projects/${projectId}/executions/${executionId}/tool-calls`,
        `/projects/${projectId}/executions/${executionId}/tool-calls`,
      ],
    ] as const) {
      await router.navigateByUrl(path);
      fixture.detectChanges();
      expect(router.url).toBe(expected);
    }
  });
});