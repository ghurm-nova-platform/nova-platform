import { DatePipe, KeyValuePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';

import { PrReviewPermissionHelper } from './pr-review-permission.helper';
import { PrReviewService } from './pr-review.service';
import {
  FindingView,
  KnowledgeReferenceView,
  RecommendationView,
  ReviewRunDetail,
  ReviewRunSummary,
  RiskScoreView,
} from './pr-review.models';
import { RiskGaugeComponent } from './risk-gauge';

@Component({
  selector: 'app-pr-review-page',
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
    MatTabsModule,
    RiskGaugeComponent,
  ],
  templateUrl: './pr-review-page.html',
  styleUrl: './pr-review-page.scss',
})
export class PrReviewPage implements OnInit {
  private readonly api = inject(PrReviewService);
  readonly permissions = inject(PrReviewPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly runs = signal<ReviewRunSummary[]>([]);
  readonly historyRuns = signal<ReviewRunSummary[]>([]);
  readonly selectedId = signal<string | null>(null);
  readonly detail = signal<ReviewRunDetail | null>(null);
  readonly findings = signal<FindingView[]>([]);
  readonly recommendations = signal<RecommendationView[]>([]);
  readonly risk = signal<RiskScoreView | null>(null);
  readonly knowledge = signal<KnowledgeReferenceView[]>([]);

  projectId = '';
  repositoryRef = '';
  sourceBranch = '';
  targetBranch = 'main';
  commitSha = '';
  pullRequestNumber: number | null = null;
  pullRequestTitle = '';
  changedFilesText = '';
  diffContent = '';

  readonly severityDistribution = computed(() => {
    const counts: Record<string, number> = {};
    for (const finding of this.findings()) {
      counts[finding.severity] = (counts[finding.severity] ?? 0) + 1;
    }
    return counts;
  });

  readonly categoryScores = computed(() => {
    const detail = this.detail();
    const risk = this.risk();
    if (risk?.categoryScores && Object.keys(risk.categoryScores).length > 0) {
      return risk.categoryScores;
    }
    if (!detail) {
      return {};
    }
    return {
      Architecture: detail.architectureScore,
      Security: detail.securityScore,
      Performance: detail.performanceScore,
      CodeQuality: detail.qualityScore,
      Testing: detail.testingScore,
      Documentation: detail.documentationScore,
    };
  });

  readonly architectureFindings = computed(() =>
    this.findings().filter((finding) => finding.category === 'Architecture'),
  );

  readonly securityFindings = computed(() =>
    this.findings().filter((finding) => finding.category === 'Security'),
  );

  readonly performanceFindings = computed(() =>
    this.findings().filter((finding) => finding.category === 'Performance'),
  );

  readonly durationMs = computed(() => {
    const d = this.detail();
    if (!d?.startedAt || !d.completedAt) {
      return null;
    }
    return new Date(d.completedAt).getTime() - new Date(d.startedAt).getTime();
  });

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      return;
    }
    this.reload();
    this.loadHistory();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(this.projectId || undefined).subscribe({
      next: (runs) => {
        this.runs.set(runs);
        if (runs.length > 0 && !this.selectedId()) {
          this.selectRun(runs[0].id);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load PR reviews');
      },
    });
  }

  loadHistory(): void {
    this.api.history(this.projectId || undefined).subscribe({
      next: (runs) => this.historyRuns.set(runs),
      error: () => this.historyRuns.set([]),
    });
  }

  selectRun(id: string): void {
    this.selectedId.set(id);
    this.loading.set(true);
    this.error.set(null);
    this.api.get(id).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.findings.set(detail.findings ?? []);
        this.recommendations.set(detail.recommendations ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load review detail');
      },
    });
    this.api.findings(id).subscribe({
      next: (items) => this.findings.set(items),
      error: () => undefined,
    });
    this.api.recommendations(id).subscribe({
      next: (items) => this.recommendations.set(items),
      error: () => undefined,
    });
    this.api.riskScore(id).subscribe({
      next: (risk) => this.risk.set(risk),
      error: () => this.risk.set(null),
    });
    this.api.knowledge(id).subscribe({
      next: (refs) => this.knowledge.set(refs),
      error: () => this.knowledge.set([]),
    });
  }

  runReview(): void {
    if (!this.permissions.canRun() || !this.projectId || !this.diffContent.trim()) {
      return;
    }
    this.loading.set(true);
    this.api
      .run({
        projectId: this.projectId,
        pullRequestNumber: this.pullRequestNumber,
        pullRequestTitle: this.pullRequestTitle || null,
        repositoryRef: this.repositoryRef || null,
        sourceBranch: this.sourceBranch || null,
        targetBranch: this.targetBranch || null,
        commitSha: this.commitSha || null,
        changedFiles: this.changedFilesText
          .split(/\r?\n/)
          .map((line) => line.trim())
          .filter(Boolean),
        diffContent: this.diffContent,
      })
      .subscribe({
        next: (detail) => {
          this.loading.set(false);
          this.reload();
          this.loadHistory();
          this.selectRun(detail.id);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to run PR review');
        },
      });
  }

  rerunSelected(): void {
    const id = this.selectedId();
    if (!id || !this.permissions.canRun()) {
      return;
    }
    this.loading.set(true);
    this.api.rerun(id).subscribe({
      next: (detail) => {
        this.loading.set(false);
        this.reload();
        this.loadHistory();
        this.selectRun(detail.id);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to rerun PR review');
      },
    });
  }

  exportSelected(format: string): void {
    const id = this.selectedId();
    if (!id || !this.permissions.canRead()) {
      return;
    }
    this.api.export(id, format).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `pr-review-${id}.${exportExtension(format)}`;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Export failed'),
    });
  }

  severityClass(severity: string): string {
    return `pr-review__severity pr-review__severity--${severity.toLowerCase()}`;
  }
}

export function exportExtension(format: string): string {
  switch (format) {
    case 'markdown':
    case 'md':
      return 'md';
    case 'json':
      return 'json';
    case 'pdf':
      return 'pdf';
    default:
      return format;
  }
}

export function findingsByCategory(findings: FindingView[], category: string): FindingView[] {
  return findings.filter((finding) => finding.category === category);
}
