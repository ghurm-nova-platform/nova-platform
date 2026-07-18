import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import {
  ModelRoutingPolicy,
  ROUTING_POLICY_STATUSES,
  RoutingPolicyStatus,
} from './model-gateway.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';
import { ModelRoutingPolicyService } from './model-routing-policy.service';

@Component({
  selector: 'app-model-routing-policies-list-page',
  imports: [
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './model-routing-policies-list-page.html',
  styleUrl: './model-gateway-page.scss',
})
export class ModelRoutingPoliciesListPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly policiesApi = inject(ModelRoutingPolicyService);
  readonly permissions = inject(ModelGatewayPermissionHelper);

  readonly projectId = signal('');
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly statusControl = new FormControl<RoutingPolicyStatus | ''>('', { nonNullable: true });
  readonly displayedColumns = ['name', 'policyKey', 'status', 'strategy', 'agentId', 'updatedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly rows = signal<ModelRoutingPolicy[]>([]);
  readonly total = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sort = signal('createdAt,desc');
  readonly statuses = ROUTING_POLICY_STATUSES;

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    this.searchControl.valueChanges.pipe(debounceTime(250), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.statusControl.valueChanges.subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
    this.load();
  }

  load(): void {
    if (!this.permissions.canReadRoutingPolicies()) {
      this.unauthorized.set(true);
      this.rows.set([]);
      return;
    }
    this.unauthorized.set(false);
    this.loading.set(true);
    this.error.set(null);
    this.policiesApi
      .listPolicies(this.projectId(), {
        search: this.searchControl.value.trim() || undefined,
        status: this.statusControl.value || undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
        sort: this.sort(),
      })
      .subscribe({
        next: (page) => {
          this.rows.set(page.content);
          this.total.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err: { status?: number }) => {
          this.loading.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set('Unable to load routing policies.');
        },
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  onSort(sort: Sort): void {
    this.sort.set(!sort.active || !sort.direction ? 'createdAt,desc' : `${sort.active},${sort.direction}`);
    this.load();
  }

  create(): void {
    void this.router.navigate(['/projects', this.projectId(), 'model-routing-policies', 'new']);
  }

  open(policy: ModelRoutingPolicy): void {
    void this.router.navigate(['/projects', this.projectId(), 'model-routing-policies', policy.id]);
  }

  activate(policy: ModelRoutingPolicy): void {
    if (
      !this.permissions.canManageRoutingPolicies() ||
      !window.confirm(`Activate routing policy "${policy.name}"?`)
    ) {
      return;
    }
    this.policiesApi.activatePolicy(this.projectId(), policy.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to activate routing policy.'),
    });
  }

  archive(policy: ModelRoutingPolicy): void {
    if (
      !this.permissions.canManageRoutingPolicies() ||
      !window.confirm(`Archive routing policy "${policy.name}"?`)
    ) {
      return;
    }
    this.policiesApi.archivePolicy(this.projectId(), policy.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Unable to archive routing policy.'),
    });
  }

  statusClass(status: RoutingPolicyStatus): string {
    return `status status--${status.toLowerCase()}`;
  }
}
