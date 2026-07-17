import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Agent } from '../agents/agent.models';
import { AgentService } from '../agents/agent.service';
import { ConversationPermissionHelper } from './conversation-permission.helper';
import { ConversationService } from './conversation.service';

@Component({
  selector: 'app-conversation-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './conversation-form-page.html',
  styleUrl: './conversation-form-page.scss',
})
export class ConversationFormPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly conversationsApi = inject(ConversationService);
  private readonly agentsApi = inject(AgentService);
  readonly permissions = inject(ConversationPermissionHelper);

  readonly projectId = signal('');
  readonly loadingAgents = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly agents = signal<Agent[]>([]);

  readonly agentControl = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required],
  });
  readonly titleControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(255)],
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    if (!this.permissions.canCreate()) {
      this.unauthorized.set(true);
      return;
    }
    const presetAgentId = this.route.snapshot.queryParamMap.get('agentId');
    if (presetAgentId) {
      this.agentControl.setValue(presetAgentId);
    }
    this.loadAgents();
  }

  save(): void {
    if (this.agentControl.invalid || this.saving()) {
      this.agentControl.markAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const title = this.titleControl.value.trim();
    this.conversationsApi
      .create(this.projectId(), {
        agentId: this.agentControl.value,
        title: title || undefined,
      })
      .subscribe({
        next: (conversation) => {
          this.saving.set(false);
          void this.router.navigate([
            '/projects',
            this.projectId(),
            'conversations',
            conversation.id,
          ]);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create conversation.');
        },
      });
  }

  private loadAgents(): void {
    this.loadingAgents.set(true);
    this.agentsApi.list(this.projectId(), { status: 'ACTIVE', size: 100, sort: 'name,asc' }).subscribe({
      next: (page) => {
        this.agents.set(page.content);
        this.loadingAgents.set(false);
        if (!this.agentControl.value && page.content.length === 1) {
          this.agentControl.setValue(page.content[0].id);
        }
      },
      error: () => {
        this.error.set('Unable to load agents.');
        this.loadingAgents.set(false);
      },
    });
  }
}
