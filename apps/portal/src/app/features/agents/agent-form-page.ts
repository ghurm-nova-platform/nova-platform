import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Agent, AgentVisibility, MODEL_PROVIDERS } from './agent.models';
import { AgentPermissionHelper } from './agent-permission.helper';
import { AgentService } from './agent.service';

@Component({
  selector: 'app-agent-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './agent-form-page.html',
  styleUrl: './agent-form-page.scss',
})
export class AgentFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agentsApi = inject(AgentService);
  readonly permissions = inject(AgentPermissionHelper);

  readonly projectId = signal('');
  readonly agentId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly conflict = signal(false);
  readonly providers = MODEL_PROVIDERS;
  readonly visibilities: AgentVisibility[] = ['PRIVATE', 'PROJECT', 'ORGANIZATION'];
  private version = 0;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    systemPrompt: ['', [Validators.required, Validators.maxLength(20000)]],
    modelProvider: ['OPENAI', Validators.required],
    modelName: ['', [Validators.required, Validators.maxLength(128)]],
    temperature: [0.2, [Validators.required, Validators.min(0), Validators.max(2)]],
    maxTokens: this.fb.control<number | null>(2048),
    visibility: this.fb.nonNullable.control<AgentVisibility>('PROJECT', Validators.required),
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    const agentId = this.route.snapshot.paramMap.get('agentId');
    this.agentId.set(agentId);
    if (agentId) {
      if (!this.permissions.canUpdate()) {
        this.error.set('You are not allowed to edit agents.');
        return;
      }
      this.loading.set(true);
      this.agentsApi.get(this.projectId(), agentId).subscribe({
        next: (agent) => {
          this.patchForm(agent);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Unable to load agent.');
          this.loading.set(false);
        },
      });
    } else if (!this.permissions.canCreate()) {
      this.error.set('You are not allowed to create agents.');
    }
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.conflict.set(false);
    const raw = this.form.getRawValue();
    const body = {
      name: raw.name.trim(),
      description: raw.description.trim() || null,
      systemPrompt: raw.systemPrompt.trim(),
      modelProvider: raw.modelProvider,
      modelName: raw.modelName.trim(),
      temperature: Number(raw.temperature),
      maxTokens: raw.maxTokens ?? null,
      visibility: raw.visibility,
    };

    const request$ = this.agentId()
      ? this.agentsApi.update(this.projectId(), this.agentId()!, { ...body, version: this.version })
      : this.agentsApi.create(this.projectId(), body);

    request$.subscribe({
      next: (agent) => {
        this.saving.set(false);
        void this.router.navigate(['/projects', this.projectId(), 'agents', agent.id]);
      },
      error: (err: { status?: number; error?: { code?: string; message?: string } }) => {
        this.saving.set(false);
        if (err.status === 409 && err.error?.code === 'OPTIMISTIC_LOCK_CONFLICT') {
          this.conflict.set(true);
          this.error.set('This agent was modified elsewhere. Reload and try again.');
          return;
        }
        this.error.set(err.error?.message ?? 'Unable to save agent.');
      },
    });
  }

  private patchForm(agent: Agent): void {
    this.version = agent.version;
    this.form.patchValue({
      name: agent.name,
      description: agent.description ?? '',
      systemPrompt: agent.systemPrompt,
      modelProvider: agent.modelProvider,
      modelName: agent.modelName,
      temperature: agent.temperature,
      maxTokens: agent.maxTokens,
      visibility: agent.visibility,
    });
  }
}
