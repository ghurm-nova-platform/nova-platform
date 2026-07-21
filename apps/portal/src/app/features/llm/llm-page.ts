import { DatePipe, KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';

import { LlmPermissionHelper } from './llm-permission.helper';
import { LlmService } from './llm.service';
import {
  LlmCompletionResponse,
  LlmConfigResponse,
  LlmConversationView,
  LlmMessageView,
  LlmModelView,
  LlmPromptView,
  LlmProviderStatusView,
} from './llm.models';

@Component({
  selector: 'app-llm-page',
  imports: [
    DatePipe,
    KeyValuePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTabsModule,
  ],
  templateUrl: './llm-page.html',
  styleUrl: './llm-page.scss',
})
export class LlmPage implements OnInit {
  private readonly api = inject(LlmService);
  readonly permissions = inject(LlmPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);

  readonly config = signal<LlmConfigResponse | null>(null);
  readonly providers = signal<LlmProviderStatusView[]>([]);
  readonly metrics = signal<Record<string, unknown>>({});
  readonly models = signal<LlmModelView[]>([]);
  readonly prompts = signal<LlmPromptView[]>([]);
  readonly conversations = signal<LlmConversationView[]>([]);
  readonly messages = signal<LlmMessageView[]>([]);
  readonly selectedConversationId = signal<string | null>(null);
  readonly lastReply = signal<LlmCompletionResponse | null>(null);
  readonly lifecycleBusyId = signal<string | null>(null);

  selectedModelId = '';
  chatTitle = 'Local chat';
  chatInput = '';

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      return;
    }
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.getConfig().subscribe({
      next: (config) => this.config.set(config),
      error: (err) => this.error.set(err?.error?.message ?? 'Failed to load LLM config'),
    });

    this.api.getHealth().subscribe({
      next: (health) => this.providers.set(health.providers ?? []),
      error: () => this.providers.set([]),
    });

    this.api.getMetrics().subscribe({
      next: (summary) => this.metrics.set(summary.metrics ?? {}),
      error: () => this.metrics.set({}),
    });

    this.api.listModels().subscribe({
      next: (models) => {
        this.models.set(models);
        if (!this.selectedModelId && models.length > 0) {
          this.selectedModelId = models[0].id;
        }
      },
      error: () => this.models.set([]),
    });

    this.api.listPrompts().subscribe({
      next: (prompts) => this.prompts.set(prompts),
      error: () => this.prompts.set([]),
    });

    this.api.listConversations().subscribe({
      next: (conversations) => {
        this.conversations.set(conversations);
        this.loading.set(false);
        if (conversations.length > 0 && !this.selectedConversationId()) {
          this.selectConversation(conversations[0].id);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load conversations');
      },
    });
  }

  refreshProviders(): void {
    this.api.refreshProviders().subscribe({
      next: (providers) => this.providers.set(providers),
      error: (err) => this.error.set(err?.error?.message ?? 'Failed to refresh providers'),
    });
  }

  selectConversation(id: string): void {
    this.selectedConversationId.set(id);
    this.api.listMessages(id).subscribe({
      next: (messages) => this.messages.set(messages),
      error: () => this.messages.set([]),
    });
  }

  startConversation(): void {
    if (!this.permissions.canInfer()) {
      return;
    }
    this.api
      .createConversation({
        modelId: this.selectedModelId || null,
        title: this.chatTitle || 'Local chat',
      })
      .subscribe({
        next: (conversation) => {
          this.conversations.update((items) => [conversation, ...items]);
          this.selectConversation(conversation.id);
        },
        error: (err) => this.error.set(err?.error?.message ?? 'Failed to create conversation'),
      });
  }

  sendChat(): void {
    if (!this.permissions.canInfer() || !this.chatInput.trim()) {
      return;
    }
    const content = this.chatInput.trim();
    this.chatInput = '';
    this.loading.set(true);
    this.api
      .chat({
        modelId: this.selectedModelId || null,
        conversationId: this.selectedConversationId(),
        messages: [{ role: 'USER', content }],
      })
      .subscribe({
        next: (reply) => {
          this.lastReply.set(reply);
          this.loading.set(false);
          if (reply.conversationId) {
            this.selectedConversationId.set(reply.conversationId);
            this.selectConversation(reply.conversationId);
            this.api.listConversations().subscribe({
              next: (items) => this.conversations.set(items),
            });
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Chat request failed');
        },
      });
  }

  runLifecycle(
    modelId: string,
    action:
      | 'install'
      | 'download'
      | 'load'
      | 'unload'
      | 'start'
      | 'stop'
      | 'restart'
      | 'warmup'
      | 'enable'
      | 'disable',
  ): void {
    if (!this.permissions.canManageModels()) {
      return;
    }
    this.lifecycleBusyId.set(modelId);
    const call =
      action === 'install'
        ? this.api.installModel(modelId)
        : action === 'download'
          ? this.api.downloadModel(modelId)
          : action === 'load'
            ? this.api.loadModel(modelId)
            : action === 'unload'
              ? this.api.unloadModel(modelId)
              : action === 'start'
                ? this.api.startModel(modelId)
                : action === 'stop'
                  ? this.api.stopModel(modelId)
                  : action === 'restart'
                    ? this.api.restartModel(modelId)
                    : action === 'warmup'
                      ? this.api.warmupModel(modelId)
                      : action === 'enable'
                        ? this.api.enableModel(modelId)
                        : this.api.disableModel(modelId);

    call.subscribe({
      next: (updated) => {
        this.models.update((items) => items.map((item) => (item.id === updated.id ? updated : item)));
        this.lifecycleBusyId.set(null);
      },
      error: (err) => {
        this.lifecycleBusyId.set(null);
        this.error.set(err?.error?.message ?? `Failed to ${action} model`);
      },
    });
  }

  providerStatusClass(status: string): string {
    return `llm__status llm__status--${status.toLowerCase()}`;
  }
}
