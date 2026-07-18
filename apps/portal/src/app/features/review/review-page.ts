import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { KeyValuePipe } from '@angular/common';

import { ReviewPermissionHelper } from './review-permission.helper';
import { ReviewService } from './review.service';
import {
  ReviewCategory,
  ReviewFinding,
  ReviewResult,
  ReviewSeverity,
  scoreTone,
} from './review.models';

@Component({
  selector: 'app-review-page',
  imports: [
    ReactiveFormsModule,
    KeyValuePipe,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './review-page.html',
  styleUrl: './review-page.scss',
})
export class ReviewPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly reviewApi = inject(ReviewService);
  readonly permissions = inject(ReviewPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<ReviewResult | null>(null);
  readonly expanded = signal<Record<string, boolean>>({});
  readonly search = signal('');
  readonly filterSeverity = signal<ReviewSeverity | ''>('');
  readonly filterCategory = signal<ReviewCategory | ''>('');
  readonly filterArtifact = signal('');

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  readonly filteredFindings = computed(() => {
    const review = this.result();
    if (!review) {
      return [] as ReviewFinding[];
    }
    const q = this.search().trim().toLowerCase();
    const severity = this.filterSeverity();
    const category = this.filterCategory();
    const artifact = this.filterArtifact();
    return review.findings.filter((finding) => {
      if (severity && finding.severity !== severity) {
        return false;
      }
      if (category && finding.category !== category) {
        return false;
      }
      if (artifact && (finding.artifactPath ?? '') !== artifact) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        finding.title.toLowerCase().includes(q) ||
        finding.description.toLowerCase().includes(q) ||
        finding.recommendation.toLowerCase().includes(q) ||
        finding.category.toLowerCase().includes(q) ||
        finding.severity.toLowerCase().includes(q) ||
        (finding.artifactPath ?? '').toLowerCase().includes(q)
      );
    });
  });

  readonly scoreClass = computed(() => {
    const review = this.result();
    return review ? scoreTone(review.score) : 'red';
  });

  readonly severityOptions: ReviewSeverity[] = ['INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly categoryOptions: ReviewCategory[] = [
    'CORRECTNESS',
    'ARCHITECTURE',
    'MAINTAINABILITY',
    'READABILITY',
    'SECURITY',
    'PERFORMANCE',
    'CONCURRENCY',
    'VALIDATION',
    'ERROR_HANDLING',
    'DOCUMENTATION',
    'NAMING',
    'TESTING',
    'BEST_PRACTICES',
  ];

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  runReview(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.reviewApi.run(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.expanded.set({});
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to run review');
      },
    });
  }

  loadLatest(): void {
    if (!this.permissions.canRead() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.reviewApi.getLatest(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.expanded.set({});
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load review');
      },
    });
  }

  toggleFinding(id: string): void {
    this.expanded.update((state) => ({ ...state, [id]: !state[id] }));
  }

  onSearch(value: string): void {
    this.search.set(value);
  }

  onSeverityFilter(value: ReviewSeverity | ''): void {
    this.filterSeverity.set(value);
  }

  onCategoryFilter(value: ReviewCategory | ''): void {
    this.filterCategory.set(value);
  }

  onArtifactFilter(value: string): void {
    this.filterArtifact.set(value);
  }
}
