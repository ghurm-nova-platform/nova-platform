import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { EmbeddingProvider, KnowledgeBase } from './knowledge.models';
import { KnowledgePermissionHelper } from './knowledge-permission.helper';
import { KnowledgeService } from './knowledge.service';

@Component({
  selector: 'app-knowledge-base-form-page',
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
  templateUrl: './knowledge-base-form-page.html',
  styleUrl: './knowledge-base-form-page.scss',
})
export class KnowledgeBaseFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly knowledgeApi = inject(KnowledgeService);
  readonly permissions = inject(KnowledgePermissionHelper);

  readonly projectId = signal('');
  readonly knowledgeBaseId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly providers = signal<EmbeddingProvider[]>([]);
  readonly currentKnowledgeBase = signal<KnowledgeBase | null>(null);

  readonly form = this.fb.nonNullable.group({
    knowledgeKey: [
      '',
      [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/), Validators.maxLength(100)],
    ],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    embeddingProviderKey: ['', Validators.required],
    chunkSize: [1000, [Validators.required, Validators.min(100), Validators.max(5000)]],
    chunkOverlap: [150, [Validators.required, Validators.min(0), Validators.max(4999)]],
    defaultTopK: [5, [Validators.required, Validators.min(1), Validators.max(20)]],
    minimumScore: [''],
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    const knowledgeBaseId = this.route.snapshot.paramMap.get('knowledgeBaseId');
    this.knowledgeBaseId.set(knowledgeBaseId);
    this.editMode.set(!!knowledgeBaseId && knowledgeBaseId !== 'new');

    if (this.editMode() && knowledgeBaseId) {
      if (!this.permissions.canUpdate()) {
        this.unauthorized.set(true);
        return;
      }
      this.form.controls.knowledgeKey.disable();
      this.loadKnowledgeBase(knowledgeBaseId);
    } else if (!this.permissions.canCreate()) {
      this.unauthorized.set(true);
    }

    this.loadProviders();
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    const minimumScore = this.parseMinimumScore(raw.minimumScore);

    if (this.editMode() && this.knowledgeBaseId()) {
      const current = this.currentKnowledgeBase();
      if (!current) {
        return;
      }
      this.knowledgeApi
        .updateKnowledgeBase(this.projectId(), this.knowledgeBaseId()!, {
          version: current.version,
          name: raw.name.trim(),
          description: raw.description.trim() || null,
          embeddingProviderKey: raw.embeddingProviderKey,
          chunkSize: raw.chunkSize,
          chunkOverlap: raw.chunkOverlap,
          defaultTopK: raw.defaultTopK,
          minimumScore,
        })
        .subscribe({
          next: (knowledgeBase) => {
            this.saving.set(false);
            this.currentKnowledgeBase.set(knowledgeBase);
            void this.router.navigate([
              '/projects',
              this.projectId(),
              'knowledge-bases',
              knowledgeBase.id,
            ]);
          },
          error: (err: { status?: number; error?: { message?: string } }) => {
            this.saving.set(false);
            if (err.status === 403) {
              this.unauthorized.set(true);
              return;
            }
            this.error.set(err.error?.message ?? 'Unable to save knowledge base.');
          },
        });
      return;
    }

    this.knowledgeApi
      .createKnowledgeBase(this.projectId(), {
        knowledgeKey: raw.knowledgeKey.trim(),
        name: raw.name.trim(),
        description: raw.description.trim() || null,
        embeddingProviderKey: raw.embeddingProviderKey,
        chunkSize: raw.chunkSize,
        chunkOverlap: raw.chunkOverlap,
        defaultTopK: raw.defaultTopK,
        minimumScore,
      })
      .subscribe({
        next: (knowledgeBase) => {
          this.saving.set(false);
          void this.router.navigate([
            '/projects',
            this.projectId(),
            'knowledge-bases',
            knowledgeBase.id,
          ]);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create knowledge base.');
        },
      });
  }

  activate(): void {
    const current = this.currentKnowledgeBase();
    if (
      !current ||
      !this.permissions.canActivate() ||
      !window.confirm(`Activate "${current.name}"?`)
    ) {
      return;
    }
    this.knowledgeApi.activateKnowledgeBase(this.projectId(), current.id).subscribe({
      next: (knowledgeBase) => this.currentKnowledgeBase.set(knowledgeBase),
      error: () => this.error.set('Unable to activate knowledge base.'),
    });
  }

  archive(): void {
    const current = this.currentKnowledgeBase();
    if (
      !current ||
      !this.permissions.canArchive() ||
      !window.confirm(`Archive "${current.name}"?`)
    ) {
      return;
    }
    this.knowledgeApi.archiveKnowledgeBase(this.projectId(), current.id).subscribe({
      next: () => void this.router.navigate(['/projects', this.projectId(), 'knowledge-bases']),
      error: () => this.error.set('Unable to archive knowledge base.'),
    });
  }

  providerLabel(provider: EmbeddingProvider): string {
    return `${provider.providerKey} (${provider.model}, ${provider.dimensions}d)`;
  }

  statusClass(status: KnowledgeBase['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadKnowledgeBase(knowledgeBaseId: string): void {
    this.loading.set(true);
    this.knowledgeApi.getKnowledgeBase(this.projectId(), knowledgeBaseId).subscribe({
      next: (knowledgeBase) => {
        this.currentKnowledgeBase.set(knowledgeBase);
        this.form.patchValue({
          knowledgeKey: knowledgeBase.knowledgeKey,
          name: knowledgeBase.name,
          description: knowledgeBase.description ?? '',
          embeddingProviderKey: knowledgeBase.embeddingProviderKey,
          chunkSize: knowledgeBase.chunkSize,
          chunkOverlap: knowledgeBase.chunkOverlap,
          defaultTopK: knowledgeBase.defaultTopK,
          minimumScore:
            knowledgeBase.minimumScore !== null ? String(knowledgeBase.minimumScore) : '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load knowledge base.');
        this.loading.set(false);
      },
    });
  }

  private loadProviders(): void {
    if (!this.permissions.canRead()) {
      return;
    }
    this.knowledgeApi.listProviders(this.projectId()).subscribe({
      next: (response) => this.providers.set(response.providers),
      error: () => this.error.set('Unable to load embedding provider allowlist.'),
    });
  }

  private parseMinimumScore(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }
}
