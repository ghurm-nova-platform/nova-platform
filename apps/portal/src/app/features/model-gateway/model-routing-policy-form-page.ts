import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import {
  ModelRoutingPolicy,
  ROUTING_STRATEGIES,
  RoutingStrategy,
} from './model-gateway.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelRoutingPolicyService } from './model-routing-policy.service';

@Component({
  selector: 'app-model-routing-policy-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './model-routing-policy-form-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ModelRoutingPolicyFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly policiesApi = inject(ModelRoutingPolicyService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly projectId = signal('');
  readonly policyId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly currentPolicy = signal<ModelRoutingPolicy | null>(null);
  readonly strategies = ROUTING_STRATEGIES;

  readonly form = this.fb.nonNullable.group({
    policyKey: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/), Validators.maxLength(100)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    agentId: [''],
    strategy: ['PRIORITY_FALLBACK' as RoutingStrategy, Validators.required],
    fallbackEnabled: [true],
    retryEnabled: [true],
    maximumProviderAttempts: [2, [Validators.required, Validators.min(1), Validators.max(10)]],
    maximumTotalDurationMs: [120000, [Validators.required, Validators.min(1000), Validators.max(600000)]],
    requireToolSupport: [false],
    requireKnowledgeSupport: [false],
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    const policyId = this.route.snapshot.paramMap.get('policyId');
    this.policyId.set(policyId);
    this.editMode.set(!!policyId && policyId !== 'new');

    if (this.editMode() && policyId) {
      if (!this.permissions.canManageRoutingPolicies()) {
        this.unauthorized.set(true);
        return;
      }
      this.form.controls.policyKey.disable();
      this.loadPolicy(policyId);
    } else if (!this.permissions.canManageRoutingPolicies()) {
      this.unauthorized.set(true);
    }
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    const agentId = raw.agentId.trim() || null;

    if (this.editMode() && this.policyId()) {
      const current = this.currentPolicy();
      if (!current) {
        return;
      }
      this.policiesApi
        .updatePolicy(this.projectId(), this.policyId()!, {
          version: current.version,
          name: raw.name.trim(),
          description: raw.description.trim() || null,
          agentId,
          strategy: raw.strategy,
          fallbackEnabled: raw.fallbackEnabled,
          retryEnabled: raw.retryEnabled,
          maximumProviderAttempts: raw.maximumProviderAttempts,
          maximumTotalDurationMs: raw.maximumTotalDurationMs,
          requireToolSupport: raw.requireToolSupport,
          requireKnowledgeSupport: raw.requireKnowledgeSupport,
        })
        .subscribe({
          next: (policy) => {
            this.saving.set(false);
            this.currentPolicy.set(policy);
          },
          error: (err: { status?: number; error?: { message?: string } }) => {
            this.saving.set(false);
            if (err.status === 403) {
              this.unauthorized.set(true);
              return;
            }
            this.error.set(err.error?.message ?? 'Unable to save routing policy.');
          },
        });
      return;
    }

    this.policiesApi
      .createPolicy(this.projectId(), {
        policyKey: raw.policyKey.trim(),
        name: raw.name.trim(),
        description: raw.description.trim() || null,
        agentId,
        strategy: raw.strategy,
        fallbackEnabled: raw.fallbackEnabled,
        retryEnabled: raw.retryEnabled,
        maximumProviderAttempts: raw.maximumProviderAttempts,
        maximumTotalDurationMs: raw.maximumTotalDurationMs,
        requireToolSupport: raw.requireToolSupport,
        requireKnowledgeSupport: raw.requireKnowledgeSupport,
      })
      .subscribe({
        next: (policy) => {
          this.saving.set(false);
          void this.router.navigate(['/projects', this.projectId(), 'model-routing-policies', policy.id]);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create routing policy.');
        },
      });
  }

  activate(): void {
    const current = this.currentPolicy();
    if (!current || !this.permissions.canManageRoutingPolicies() || !window.confirm(`Activate "${current.name}"?`)) {
      return;
    }
    this.policiesApi.activatePolicy(this.projectId(), current.id).subscribe({
      next: (policy) => this.currentPolicy.set(policy),
      error: () => this.error.set('Unable to activate routing policy.'),
    });
  }

  archive(): void {
    const current = this.currentPolicy();
    if (!current || !this.permissions.canManageRoutingPolicies() || !window.confirm(`Archive "${current.name}"?`)) {
      return;
    }
    this.policiesApi.archivePolicy(this.projectId(), current.id).subscribe({
      next: () => void this.router.navigate(['/projects', this.projectId(), 'model-routing-policies']),
      error: () => this.error.set('Unable to archive routing policy.'),
    });
  }

  statusClass(status: ModelRoutingPolicy['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadPolicy(policyId: string): void {
    this.loading.set(true);
    this.policiesApi.getPolicy(this.projectId(), policyId).subscribe({
      next: (policy) => {
        this.currentPolicy.set(policy);
        this.form.patchValue({
          policyKey: policy.policyKey,
          name: policy.name,
          description: policy.description ?? '',
          agentId: policy.agentId ?? '',
          strategy: policy.strategy,
          fallbackEnabled: policy.fallbackEnabled,
          retryEnabled: policy.retryEnabled,
          maximumProviderAttempts: policy.maximumProviderAttempts,
          maximumTotalDurationMs: policy.maximumTotalDurationMs,
          requireToolSupport: policy.requireToolSupport,
          requireKnowledgeSupport: policy.requireKnowledgeSupport,
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load routing policy.');
        this.loading.set(false);
      },
    });
  }
}
