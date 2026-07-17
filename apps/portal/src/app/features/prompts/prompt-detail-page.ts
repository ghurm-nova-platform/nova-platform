import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Prompt, PromptStatus } from './prompt.models';
import { PromptPermissionHelper } from './prompt-permission.helper';
import { PromptService } from './prompt.service';

@Component({
  selector: 'app-prompt-detail-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './prompt-detail-page.html',
  styleUrl: './prompt-detail-page.scss',
})
export class PromptDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly promptsApi = inject(PromptService);
  readonly permissions = inject(PromptPermissionHelper);

  readonly projectId = signal('');
  readonly promptId = signal('');
  readonly prompt = signal<Prompt | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly publishing = signal(false);

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.promptId.set(this.route.snapshot.paramMap.get('promptId') ?? '');
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.promptsApi.get(this.projectId(), this.promptId()).subscribe({
      next: (prompt) => {
        this.prompt.set(prompt);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load prompt details.');
        this.loading.set(false);
      },
    });
  }

  edit(): void {
    void this.router.navigate(['/projects', this.projectId(), 'prompts', this.promptId(), 'edit']);
  }

  archive(): void {
    const current = this.prompt();
    if (!current || !window.confirm(`Archive prompt "${current.name}"?`)) {
      return;
    }
    this.promptsApi.archive(this.projectId(), this.promptId()).subscribe({
      next: () => this.reload(),
      error: () => this.error.set('Unable to archive prompt.'),
    });
  }

  publishDraft(): void {
    const current = this.prompt();
    const draftId = current?.currentDraftVersionId;
    if (!current || !draftId || !this.permissions.canPublish()) {
      return;
    }
    if (!window.confirm(`Publish draft v${current.currentDraftVersionNumber}?`)) {
      return;
    }
    this.publishing.set(true);
    this.promptsApi.publishVersion(this.projectId(), this.promptId(), draftId, {}).subscribe({
      next: () => {
        this.publishing.set(false);
        this.reload();
      },
      error: (err: { error?: { message?: string } }) => {
        this.publishing.set(false);
        this.error.set(err.error?.message ?? 'Unable to publish draft.');
      },
    });
  }

  statusClass(status: PromptStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
