import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { DiffLine, PromptCompareResponse, PromptVersion } from './prompt.models';
import { PromptPermissionHelper } from './prompt-permission.helper';
import { PromptService } from './prompt.service';

@Component({
  selector: 'app-prompt-compare-page',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './prompt-compare-page.html',
  styleUrl: './prompt-compare-page.scss',
})
export class PromptComparePage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly promptsApi = inject(PromptService);
  readonly permissions = inject(PromptPermissionHelper);

  readonly projectId = signal('');
  readonly promptId = signal('');
  readonly versions = signal<PromptVersion[]>([]);
  readonly loading = signal(true);
  readonly comparing = signal(false);
  readonly error = signal<string | null>(null);
  readonly compareResult = signal<PromptCompareResponse | null>(null);

  readonly leftControl = new FormControl<string>('', { nonNullable: true });
  readonly rightControl = new FormControl<string>('', { nonNullable: true });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.promptId.set(this.route.snapshot.paramMap.get('promptId') ?? '');
    this.loadVersions();
  }

  loadVersions(): void {
    if (!this.permissions.canCompare()) {
      this.error.set('You are not allowed to compare prompt versions.');
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.promptsApi.listVersions(this.projectId(), this.promptId()).subscribe({
      next: (versions) => {
        this.versions.set(versions);
        const left = this.route.snapshot.queryParamMap.get('left');
        const right = this.route.snapshot.queryParamMap.get('right');
        if (left) {
          this.leftControl.setValue(left);
        } else if (versions.length > 1) {
          this.leftControl.setValue(versions[1].id);
        } else if (versions.length === 1) {
          this.leftControl.setValue(versions[0].id);
        }
        if (right) {
          this.rightControl.setValue(right);
        } else if (versions.length > 0) {
          this.rightControl.setValue(versions[0].id);
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load prompt versions.');
        this.loading.set(false);
      },
    });
  }

  compare(): void {
    const leftVersionId = this.leftControl.value;
    const rightVersionId = this.rightControl.value;
    if (!leftVersionId || !rightVersionId) {
      this.error.set('Select two versions to compare.');
      return;
    }
    if (leftVersionId === rightVersionId) {
      this.error.set('Select two different versions.');
      return;
    }
    this.comparing.set(true);
    this.error.set(null);
    this.promptsApi
      .compare(this.projectId(), this.promptId(), { leftVersionId, rightVersionId })
      .subscribe({
        next: (result) => {
          this.compareResult.set(result);
          this.comparing.set(false);
        },
        error: () => {
          this.error.set('Unable to compare prompt versions.');
          this.comparing.set(false);
        },
      });
  }

  diffClass(line: DiffLine): string {
    return `diff-line diff-line--${line.type.toLowerCase()}`;
  }

  versionLabel(version: PromptVersion): string {
    return `v${version.versionNumber} (${version.status})`;
  }
}
