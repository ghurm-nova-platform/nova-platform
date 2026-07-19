import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { ReleasePermissionHelper } from './release-permission.helper';
import { ReleaseService } from './release.service';
import { Release, VersionBump } from './release.models';

@Component({
  selector: 'app-release-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './release-page.html',
  styleUrl: './release-page.scss',
})
export class ReleasePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly releaseApi = inject(ReleaseService);
  readonly permissions = inject(ReleasePermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly selected = signal<Release | null>(null);
  readonly releases = signal<Release[]>([]);
  readonly copied = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    projectId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    releaseName: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    bumpType: ['PATCH' as VersionBump, Validators.required],
    semanticVersion: [''],
    mergeOperationIds: [''],
    commitShas: [''],
  });

  readonly includedPrs = computed(() =>
    (this.selected()?.contents ?? []).filter((c) => c.contentType === 'PULL_REQUEST'),
  );
  readonly includedCommits = computed(() =>
    (this.selected()?.contents ?? []).filter((c) => c.contentType === 'COMMIT'),
  );

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  createRelease(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.runSingle(
      () =>
        this.releaseApi.create({
          projectId: value.projectId,
          releaseName: value.releaseName,
          description: value.description || undefined,
          bumpType: value.bumpType,
          semanticVersion: value.semanticVersion || undefined,
          mergeOperationIds: splitIds(value.mergeOperationIds),
          commitShas: splitIds(value.commitShas),
        }),
      'Failed to create release',
      true,
    );
  }

  prepareSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.releaseApi.prepare(id), 'Failed to prepare release', true);
  }

  publishSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.releaseApi.publish(id), 'Failed to publish release', true);
  }

  loadList(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    const projectId = this.form.controls.projectId.value;
    this.loading.set(true);
    this.error.set(null);
    this.releaseApi.list(projectId || undefined).subscribe({
      next: (items) => {
        this.releases.set(items);
        if (items.length > 0 && !this.selected()) {
          this.selected.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load releases');
      },
    });
  }

  loadHistory(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRead() || this.loading()) {
      return;
    }
    this.runSingle(() => this.releaseApi.history(id), 'Failed to load release history', false);
  }

  selectRelease(release: Release): void {
    this.selected.set(release);
  }

  copy(value: string, label: string): void {
    void navigator.clipboard.writeText(value).then(() => {
      this.copied.set(label);
      setTimeout(() => this.copied.set(null), 1500);
    });
  }

  private runSingle(
    call: () => ReturnType<ReleaseService['create']>,
    fallback: string,
    refreshList: boolean,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    call().subscribe({
      next: (release) => {
        this.selected.set(release);
        this.loading.set(false);
        if (refreshList) {
          this.loadList();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? fallback);
      },
    });
  }
}

function splitIds(raw: string): string[] | undefined {
  const parts = raw
    .split(/[\s,]+/)
    .map((s) => s.trim())
    .filter(Boolean);
  return parts.length ? parts : undefined;
}
