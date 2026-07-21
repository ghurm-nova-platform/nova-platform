import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { LlmPage } from './llm-page';
import { LlmService } from './llm.service';
import {
  LlmConfigResponse,
  LlmConversationView,
  LlmModelView,
  LlmPromptView,
  LlmProviderStatusView,
} from './llm.models';

describe('LlmPage', () => {
  let fixture: ComponentFixture<LlmPage>;

  const config: LlmConfigResponse = {
    enabled: true,
    defaultProvider: 'DETERMINISTIC',
    fallbackToDeterministic: true,
    timeoutSeconds: 60,
    ollamaEnabled: false,
    llamacppEnabled: false,
    vllmEnabled: false,
  };

  const provider: LlmProviderStatusView = {
    providerType: 'DETERMINISTIC',
    status: 'HEALTHY',
    endpointUrl: null,
    lastHealthCheckAt: new Date().toISOString(),
    lastError: null,
  };

  const model: LlmModelView = {
    id: 'model-1',
    organizationId: 'org-1',
    code: 'local-demo',
    displayName: 'Local Demo',
    family: 'CUSTOM',
    providerType: 'DETERMINISTIC',
    status: 'READY',
    enabled: true,
    contextLength: 4096,
    endpointUrl: null,
    owner: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const prompt: LlmPromptView = {
    id: 'prompt-1',
    code: 'chat-default',
    name: 'Default chat',
    category: 'CHAT',
    systemPrompt: 'You are helpful.',
    userPromptTemplate: 'Answer: {{question}}',
    assistantPromptTemplate: null,
    variablesJson: '{}',
    templateVersion: 1,
    enabled: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const conversation: LlmConversationView = {
    id: 'conv-1',
    modelId: 'model-1',
    projectId: null,
    title: 'Demo chat',
    status: 'ACTIVE',
    summary: null,
    tokenUsageInput: 10,
    tokenUsageOutput: 20,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LlmPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({
              roles: ['USER'],
              permissions: ['LLM_READ', 'LLM_INFER', 'LLM_MODEL_ADMIN'],
            }),
          },
        },
        {
          provide: LlmService,
          useValue: {
            getConfig: () => of(config),
            getHealth: () => of({ providers: [provider] }),
            getMetrics: () => of({ metrics: { requests: 3, successes: 2 } }),
            listModels: () => of([model]),
            listPrompts: () => of([prompt]),
            listConversations: () => of([conversation]),
            listMessages: () => of([]),
            refreshProviders: () => of([provider]),
            createConversation: () => of(conversation),
            chat: () =>
              of({
                content: 'hello',
                inputTokens: 1,
                outputTokens: 1,
                latencyMs: 5,
                providerType: 'DETERMINISTIC',
                finishReason: 'stop',
                conversationId: 'conv-1',
                cancelToken: null,
              }),
            installModel: () => of(model),
            downloadModel: () => of(model),
            loadModel: () => of(model),
            unloadModel: () => of(model),
            startModel: () => of(model),
            stopModel: () => of(model),
            restartModel: () => of(model),
            warmupModel: () => of(model),
            enableModel: () => of(model),
            disableModel: () => of(model),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LlmPage);
    fixture.detectChanges();
  });

  it('renders Local LLM Runtime heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Local LLM Runtime');
  });

  it('renders provider health and metrics on dashboard', () => {
    expect(fixture.nativeElement.textContent).toContain('Provider health');
    expect(fixture.nativeElement.textContent).toContain('DETERMINISTIC');
    expect(fixture.nativeElement.textContent).toContain('HEALTHY');
    expect(fixture.nativeElement.textContent).toContain('requests');
  });
});
