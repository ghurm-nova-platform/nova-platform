import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { CodingPermissionHelper } from './coding-permission.helper';
import { CodingService } from './coding.service';
import {
  ArtifactLanguage,
  ArtifactType,
  CodingResult,
  DiffLine,
  GeneratedArtifact,
  buildLineDiff,
} from './coding.models';

@Component({
  selector: 'app-coding-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './coding-page.html',
  styleUrl: './coding-page.scss',
})
export class CodingPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly codingApi = inject(CodingService);
  readonly permissions = inject(CodingPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<CodingResult | null>(null);
  readonly artifacts = signal<GeneratedArtifact[]>([]);
  readonly selected = signal<GeneratedArtifact | null>(null);
  readonly search = signal('');
  readonly filterLanguage = signal<ArtifactLanguage | ''>('');
  readonly filterType = signal<ArtifactType | ''>('');
  readonly copied = signal(false);
  readonly baselineContent = signal('');

  readonly columns = ['filename', 'language', 'artifactType', 'path'];

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  readonly filteredArtifacts = computed(() => {
    const q = this.search().trim().toLowerCase();
    const lang = this.filterLanguage();
    const type = this.filterType();
    return this.artifacts().filter((a) => {
      if (lang && a.language !== lang) {
        return false;
      }
      if (type && a.artifactType !== type) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        a.filename.toLowerCase().includes(q) ||
        a.path.toLowerCase().includes(q) ||
        a.language.toLowerCase().includes(q) ||
        a.artifactType.toLowerCase().includes(q)
      );
    });
  });

  readonly diffLines = computed<DiffLine[]>(() => {
    const current = this.selected()?.content ?? '';
    return buildLineDiff(this.baselineContent(), current);
  });

  readonly languageOptions: ArtifactLanguage[] = [
    'JAVA',
    'KOTLIN',
    'TYPESCRIPT',
    'JAVASCRIPT',
    'ANGULAR',
    'HTML',
    'CSS',
    'SCSS',
    'SQL',
    'ORACLE_SQL',
    'POSTGRESQL',
    'MYSQL',
    'PYTHON',
    'GO',
    'CSHARP',
    'MARKDOWN',
    'JSON',
    'YAML',
    'XML',
    'SHELL',
  ];

  readonly typeOptions: ArtifactType[] = [
    'SOURCE_FILE',
    'PATCH',
    'TEST',
    'DOCUMENTATION',
    'CONFIGURATION',
    'SQL_MIGRATION',
    'README',
  ];

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canGenerate()) {
      this.unauthorized.set(true);
    }
  }

  generate(): void {
    if (!this.permissions.canGenerate() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const taskId = this.form.controls.taskId.value.trim();
    this.codingApi.generate({ taskId }).subscribe({
      next: (result) => {
        this.result.set(result);
        this.artifacts.set(result.artifacts);
        this.selected.set(result.artifacts[0] ?? null);
        this.baselineContent.set('');
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to generate code');
      },
    });
  }

  loadArtifacts(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const taskId = this.form.controls.taskId.value.trim();
    this.codingApi.listArtifacts(taskId).subscribe({
      next: (list) => {
        this.artifacts.set(list);
        this.selected.set(list[0] ?? null);
        this.result.set(null);
        this.baselineContent.set('');
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load artifacts');
      },
    });
  }

  selectArtifact(artifact: GeneratedArtifact): void {
    this.selected.set(artifact);
  }

  onSearch(value: string): void {
    this.search.set(value);
  }

  onLanguageFilter(value: ArtifactLanguage | ''): void {
    this.filterLanguage.set(value);
  }

  onTypeFilter(value: ArtifactType | ''): void {
    this.filterType.set(value);
  }

  onBaseline(value: string): void {
    this.baselineContent.set(value);
  }

  async copySelected(): Promise<void> {
    const content = this.selected()?.content;
    if (!content || !navigator.clipboard) {
      return;
    }
    await navigator.clipboard.writeText(content);
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 1500);
  }

  downloadSelected(): void {
    const artifact = this.selected();
    if (!artifact) {
      return;
    }
    const blob = new Blob([artifact.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = artifact.filename || 'artifact.txt';
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
