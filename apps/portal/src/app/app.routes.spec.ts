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
      'KNOWLEDGE_READ',
      'KNOWLEDGE_CREATE',
      'KNOWLEDGE_UPDATE',
      'KNOWLEDGE_ACTIVATE',
      'KNOWLEDGE_ARCHIVE',
      'KNOWLEDGE_DOCUMENT_UPLOAD',
      'KNOWLEDGE_DOCUMENT_READ',
      'KNOWLEDGE_DOCUMENT_ARCHIVE',
      'KNOWLEDGE_DOCUMENT_REPROCESS',
      'KNOWLEDGE_ASSIGN',
      'KNOWLEDGE_RETRIEVE',
      'KNOWLEDGE_AUDIT_READ',
      'MODEL_PROVIDER_READ',
      'MODEL_PROVIDER_CREATE',
      'MODEL_PROVIDER_UPDATE',
      'MODEL_PROVIDER_ACTIVATE',
      'MODEL_PROVIDER_DISABLE',
      'MODEL_PROVIDER_ARCHIVE',
      'MODEL_READ',
      'MODEL_CREATE',
      'MODEL_UPDATE',
      'MODEL_ACTIVATE',
      'MODEL_DISABLE',
      'MODEL_ARCHIVE',
      'MODEL_PROJECT_ASSIGN',
      'MODEL_AGENT_ASSIGN',
      'MODEL_ROUTE_READ',
      'MODEL_ROUTE_MANAGE',
      'MODEL_USAGE_READ',
      'PROVIDER_SECRET_READ',
      'PROVIDER_SECRET_CREATE',
      'PROVIDER_SECRET_ROTATE',
      'PROVIDER_SECRET_REVOKE',
      'PROVIDER_CONNECTION_TEST',
      'MODEL_CATALOG_READ',
      'MODEL_CATALOG_CREATE',
      'MODEL_CATALOG_UPDATE',
      'MODEL_CATALOG_DELETE',
      'MODEL_CATALOG_SYNC',
      'MODEL_ALIAS_MANAGE',
      'MODEL_CAPABILITY_MANAGE',
      'ORCHESTRATION_RUN_READ',
      'ORCHESTRATION_RUN_CREATE',
      'ORCHESTRATION_RUN_UPDATE',
      'ORCHESTRATION_RUN_START',
      'ORCHESTRATION_RUN_CANCEL',
      'ORCHESTRATION_RUN_ARCHIVE',
      'ORCHESTRATION_TASK_MANAGE',
      'ORCHESTRATION_TASK_EXECUTE',
      'ORCHESTRATION_EVENT_READ',
      'PLANNER_PLAN',
      'PLANNER_IMPORT',
      'PLANNER_TEMPLATE_READ',
      'PLANNER_TEMPLATE_MANAGE',
      'CODING_GENERATE',
      'CODING_READ',
      'REVIEW_RUN',
      'REVIEW_READ',
      'TESTING_RUN',
      'TESTING_READ',
      'PATCH_RUN',
      'PATCH_READ',
      'GIT_RUN',
      'GIT_READ',
      'PR_RUN',
      'PR_READ',
      'CI_RUN',
      'CI_READ',
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
      '/model-providers',
      '/ai-models',
      '/provider-secrets',
      '/planner',
      '/coding',
      '/review',
      '/testing',
      '/patch',
      '/git',
      '/pull-requests',
      '/ci',
      '/repair',
      '/approval-gate',
      '/merge',
      '/releases',
      '/deployments',
      '/rollbacks',
      '/policies',
      '/orchestration-runs',
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
    const knowledgeBaseId = '88888888-8888-8888-8888-888888888801';
    const documentId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';
    const executionId = '99999999-9999-9999-9999-999999999901';

    const providerId = '99999999-9999-9999-9999-999999999901';
    const modelId = '99999999-9999-9999-9999-999999999911';
    const policyId = '99999999-9999-9999-9999-999999999941';
    const secretId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';
    const runId = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01';

    for (const [path, expected] of [
      ['/model-providers/new', '/model-providers/new'],
      [`/model-providers/${providerId}`, `/model-providers/${providerId}`],
      [`/model-providers/${providerId}/edit`, `/model-providers/${providerId}/edit`],
      [`/model-providers/${providerId}/models`, `/model-providers/${providerId}/models`],
      [`/model-providers/${providerId}/models/new`, `/model-providers/${providerId}/models/new`],
      [`/model-providers/${providerId}/models/${modelId}`, `/model-providers/${providerId}/models/${modelId}`],
      ['/ai-models/new', '/ai-models/new'],
      [`/ai-models/${modelId}`, `/ai-models/${modelId}`],
      [`/ai-models/${modelId}/edit`, `/ai-models/${modelId}/edit`],
      ['/provider-secrets/new', '/provider-secrets/new'],
      [`/provider-secrets/${secretId}`, `/provider-secrets/${secretId}`],
      ['/orchestration-runs/new', '/orchestration-runs/new'],
      [`/orchestration-runs/${runId}`, `/orchestration-runs/${runId}`],
      [`/orchestration-runs/${runId}/edit`, `/orchestration-runs/${runId}/edit`],
      [`/orchestration-runs/${runId}/graph`, `/orchestration-runs/${runId}/graph`],
      [`/projects/${projectId}/models`, `/projects/${projectId}/models`],
      [`/projects/${projectId}/agents/${agentId}/models`, `/projects/${projectId}/agents/${agentId}/models`],
      [
        `/projects/${projectId}/model-routing-policies`,
        `/projects/${projectId}/model-routing-policies`,
      ],
      [
        `/projects/${projectId}/model-routing-policies/new`,
        `/projects/${projectId}/model-routing-policies/new`,
      ],
      [
        `/projects/${projectId}/model-routing-policies/${policyId}`,
        `/projects/${projectId}/model-routing-policies/${policyId}`,
      ],
      [`/projects/${projectId}/model-usage`, `/projects/${projectId}/model-usage`],
      [`/projects/${projectId}/knowledge-bases`, `/projects/${projectId}/knowledge-bases`],
      [`/projects/${projectId}/knowledge-bases/new`, `/projects/${projectId}/knowledge-bases/new`],
      [
        `/projects/${projectId}/knowledge-bases/${knowledgeBaseId}`,
        `/projects/${projectId}/knowledge-bases/${knowledgeBaseId}`,
      ],
      [
        `/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents`,
        `/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents`,
      ],
      [
        `/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`,
        `/projects/${projectId}/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`,
      ],
      [
        `/projects/${projectId}/agents/${agentId}/knowledge-bases`,
        `/projects/${projectId}/agents/${agentId}/knowledge-bases`,
      ],
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
  }, 60000);
});