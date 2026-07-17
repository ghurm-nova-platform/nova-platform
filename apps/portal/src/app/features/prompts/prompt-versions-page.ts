import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PromptVersion } from './prompt.models';
import { PromptPermissionHelper } from './prompt-permission.helper';
import { PromptService } from './prompt.service';

@Component({
  selector: 'app-prompt-versions-page',
  imports: [
    DatePipe,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './prompt-versions-page.html',
  styleUrl: './prompt-versions-page.scss',
})
export class PromptVersionsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly promptsApi = inject(PromptService);
  readonly permissions = inject(PromptPermissionHelper);

  readonly projectId = signal('');
  readonly promptId = signal('');
  readonly versions = signal<PromptVersion[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly displayedColumns = ['versionNumber', 'status', 'changeSummary', 'createdAt', 'publishedAt', 'actions'];

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.promptId.set(this.route.snapshot.paramMap.get('promptId') ?? '');
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.promptsApi.listVersions(this.projectId(), this.promptId()).subscribe({
      next: (versions) => {
        this.versions.set(versions);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load prompt versions.');
        this.loading.set(false);
      },
    });
  }

  publish(version: PromptVersion): void {
    if (!this.permissions.canPublish() || version.status !== 'DRAFT') {
      return;
    }
    if (!window.confirm(`Publish version v${version.versionNumber}?`)) {
      return;
    }
    this.promptsApi
      .publishVersion(this.projectId(), this.promptId(), version.id, {})
      .subscribe({
        next: () => this.load(),
        error: () => this.error.set('Unable to publish version.'),
      });
  }

  rollback(version: PromptVersion): void {
    if (!this.permissions.canPublish()) {
      return;
    }
    if (!window.confirm(`Rollback prompt to version v${version.versionNumber}?`)) {
      return;
    }
    this.promptsApi
      .rollback(this.projectId(), this.promptId(), { sourceVersionId: version.id })
      .subscribe({
        next: () => this.load(),
        error: () => this.error.set('Unable to rollback prompt.'),
      });
  }

  compare(version: PromptVersion): void {
    void this.router.navigate(['/projects', this.projectId(), 'prompts', this.promptId(), 'compare'], {
      queryParams: { left: version.id },
    });
  }

  statusClass(status: PromptVersion['status']): string {
    return `status status--${status.toLowerCase()}`;
  }
}
