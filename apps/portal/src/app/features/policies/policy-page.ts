import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { PolicyPermissionHelper } from './policy-permission.helper';
import { PolicyService } from './policy.service';
import { EvaluationMode, Policy, PolicyType } from './policy.models';

@Component({
  selector: 'app-policy-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './policy-page.html',
  styleUrl: './policy-page.scss',
})
export class PolicyPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly policyApi = inject(PolicyService);
  readonly permissions = inject(PolicyPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly selected = signal<Policy | null>(null);
  readonly policies = signal<Policy[]>([]);

  readonly form = this.fb.nonNullable.group({
    projectId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
    policyName: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', Validators.maxLength(2000)],
    policyType: ['SEMANTIC_VERSION_REQUIRED' as PolicyType, Validators.required],
    evaluationMode: ['ALL_REQUIRED' as EvaluationMode, Validators.required],
    priority: [100, [Validators.required, Validators.min(1)]],
    releaseId: ['', [Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  create(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.runSingle(
      () =>
        this.policyApi.create({
          projectId: value.projectId,
          policyName: value.policyName,
          description: value.description || undefined,
          policyType: value.policyType,
          evaluationMode: value.evaluationMode,
          priority: value.priority,
        }),
      'Failed to create policy',
      true,
    );
  }

  evaluateSelected(): void {
    const policy = this.selected();
    const releaseId = this.form.controls.releaseId.value;
    if (!policy || !releaseId || !this.permissions.canRun() || this.loading()) {
      this.form.controls.releaseId.markAsTouched();
      return;
    }
    this.runSingle(
      () => this.policyApi.evaluate(policy.id, { releaseId }),
      'Failed to evaluate policy',
      true,
    );
  }

  enableSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.policyApi.enable(id), 'Failed to enable policy', true);
  }

  disableSelected(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRun() || this.loading()) {
      return;
    }
    this.runSingle(() => this.policyApi.disable(id), 'Failed to disable policy', true);
  }

  loadList(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    const projectId = this.form.controls.projectId.value || undefined;
    this.loading.set(true);
    this.error.set(null);
    this.policyApi.list(projectId).subscribe({
      next: (items) => {
        this.policies.set(items);
        if (items.length > 0 && !this.selected()) {
          this.selected.set(items[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load policies');
      },
    });
  }

  loadHistory(): void {
    const id = this.selected()?.id;
    if (!id || !this.permissions.canRead() || this.loading()) {
      return;
    }
    this.runSingle(() => this.policyApi.history(id), 'Failed to load history', false);
  }

  selectPolicy(item: Policy): void {
    this.selected.set(item);
  }

  private runSingle(
    call: () => ReturnType<PolicyService['create']>,
    fallback: string,
    refreshList: boolean,
  ): void {
    this.loading.set(true);
    this.error.set(null);
    call().subscribe({
      next: (policy) => {
        this.selected.set(policy);
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
