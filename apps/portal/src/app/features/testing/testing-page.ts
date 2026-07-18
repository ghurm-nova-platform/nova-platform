import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { KeyValuePipe } from '@angular/common';

import { TestingPermissionHelper } from './testing-permission.helper';
import { TestingService } from './testing.service';
import {
  GeneratedTest,
  TestPriority,
  TestType,
  TestingResult,
  coverageTone,
} from './testing.models';

@Component({
  selector: 'app-testing-page',
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
  templateUrl: './testing-page.html',
  styleUrl: './testing-page.scss',
})
export class TestingPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly testingApi = inject(TestingService);
  readonly permissions = inject(TestingPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly result = signal<TestingResult | null>(null);
  readonly expanded = signal<Record<string, boolean>>({});
  readonly search = signal('');
  readonly filterType = signal<TestType | ''>('');
  readonly filterPriority = signal<TestPriority | ''>('');

  readonly form = this.fb.nonNullable.group({
    taskId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]],
  });

  readonly filteredTests = computed(() => {
    const review = this.result();
    if (!review) {
      return [] as GeneratedTest[];
    }
    const q = this.search().trim().toLowerCase();
    const type = this.filterType();
    const priority = this.filterPriority();
    return review.generatedTests.filter((test) => {
      if (type && test.type !== type) {
        return false;
      }
      if (priority && test.priority !== priority) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        test.title.toLowerCase().includes(q) ||
        test.description.toLowerCase().includes(q) ||
        test.type.toLowerCase().includes(q) ||
        test.priority.toLowerCase().includes(q) ||
        (test.artifactPath ?? '').toLowerCase().includes(q)
      );
    });
  });

  readonly coverageClass = computed(() => {
    const review = this.result();
    return review ? coverageTone(review.coverageEstimate) : 'red';
  });

  readonly typeOptions: TestType[] = [
    'UNIT',
    'INTEGRATION',
    'API',
    'UI',
    'DATABASE',
    'SECURITY',
    'PERFORMANCE',
    'EDGE_CASE',
    'NEGATIVE',
  ];
  readonly priorityOptions: TestPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  ngOnInit(): void {
    if (!this.permissions.canRead() && !this.permissions.canRun()) {
      this.unauthorized.set(true);
    }
  }

  runTesting(): void {
    if (!this.permissions.canRun() || this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.testingApi.run(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.expanded.set({});
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to run testing agent');
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
    this.testingApi.getLatest(this.form.controls.taskId.value.trim()).subscribe({
      next: (result) => {
        this.result.set(result);
        this.expanded.set({});
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load testing result');
      },
    });
  }

  toggleTest(id: string): void {
    this.expanded.update((state) => ({ ...state, [id]: !state[id] }));
  }

  onSearch(value: string): void {
    this.search.set(value);
  }

  onTypeFilter(value: TestType | ''): void {
    this.filterType.set(value);
  }

  onPriorityFilter(value: TestPriority | ''): void {
    this.filterPriority.set(value);
  }
}
