import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { AuditPermissionHelper } from './audit-permission.helper';
import { AuditService } from './audit.service';
import { AuditEvent } from './audit.models';

@Component({
  selector: 'app-audit-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './audit-page.html',
  styleUrl: './audit-page.scss',
})
export class AuditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auditApi = inject(AuditService);
  readonly permissions = inject(AuditPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly selected = signal<AuditEvent | null>(null);
  readonly events = signal<AuditEvent[]>([]);
  readonly total = signal(0);

  readonly form = this.fb.nonNullable.group({
    projectId: [''],
    entityType: [''],
    entityId: [''],
    action: [''],
    severity: [''],
    correlationId: [''],
    requestId: [''],
  });

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
      return;
    }
    this.loadRecent();
  }

  loadRecent(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auditApi.list({ page: 0, size: 50 }).subscribe({
      next: (response) => {
        this.events.set(response.events);
        this.total.set(response.total);
        if (response.events.length > 0) {
          this.selected.set(response.events[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load audit events');
      },
    });
  }

  search(): void {
    if (!this.permissions.canRead() || this.loading()) {
      return;
    }
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.auditApi
      .search({
        projectId: value.projectId || undefined,
        entityType: (value.entityType || undefined) as AuditEvent['entityType'] | undefined,
        entityId: value.entityId || undefined,
        action: (value.action || undefined) as AuditEvent['action'] | undefined,
        severity: (value.severity || undefined) as AuditEvent['severity'] | undefined,
        correlationId: value.correlationId || undefined,
        requestId: value.requestId || undefined,
        page: 0,
        size: 50,
      })
      .subscribe({
        next: (response) => {
          this.events.set(response.events);
          this.total.set(response.total);
          this.selected.set(response.events[0] ?? null);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Audit search failed');
        },
      });
  }

  selectEvent(event: AuditEvent): void {
    this.selected.set(event);
  }
}
