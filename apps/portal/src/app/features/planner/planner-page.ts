import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { Project } from '../../core/models/catalog';
import { ProjectService } from '../projects/project.service';
import { PlannerPermissionHelper } from './planner-permission.helper';
import { PlannerService } from './planner.service';
import {
  ExecutionDependency,
  ExecutionPlan,
  ExecutionTaskDefinition,
  PlannerResponse,
  PlannerTemplate,
  TASK_TYPE_COLORS,
} from './planner.models';

interface DagNode {
  task: ExecutionTaskDefinition;
  x: number;
  y: number;
}

@Component({
  selector: 'app-planner-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DecimalPipe,
    KeyValuePipe,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './planner-page.html',
  styleUrl: './planner-page.scss',
})
export class PlannerPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly plannerApi = inject(PlannerService);
  private readonly projectsApi = inject(ProjectService);
  readonly permissions = inject(PlannerPermissionHelper);

  readonly projects = signal<Project[]>([]);
  readonly templates = signal<PlannerTemplate[]>([]);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly plannerResult = signal<PlannerResponse | null>(null);
  readonly editablePlanJson = signal('');
  readonly panX = signal(0);
  readonly panY = signal(0);
  readonly zoom = signal(1);
  readonly taskColors = TASK_TYPE_COLORS;
  readonly taskColumns = ['taskKey', 'displayName', 'taskType', 'agentRole', 'priority'];

  readonly form = this.fb.nonNullable.group({
    projectId: ['', Validators.required],
    objective: ['', [Validators.required, Validators.maxLength(4000)]],
    runName: ['', Validators.maxLength(255)],
    templateId: [''],
    metadataJson: ['', Validators.maxLength(100000)],
  });

  readonly dagNodes = computed(() => this.layoutNodes(this.currentPlan()?.tasks ?? []));
  readonly dagEdges = computed(() => this.currentPlan()?.dependencies ?? []);

  ngOnInit(): void {
    if (!this.permissions.canPlan()) {
      this.unauthorized.set(true);
      return;
    }
    this.projectsApi.list({ size: 100 }).subscribe({
      next: (page) => {
        this.projects.set(page.content ?? []);
        if (page.content?.length) {
          this.form.controls.projectId.setValue(page.content[0].id);
          this.loadTemplates(page.content[0].id);
        }
      },
      error: () => this.error.set('Failed to load projects'),
    });
    this.form.controls.projectId.valueChanges.subscribe((projectId) => {
      if (projectId) {
        this.loadTemplates(projectId);
      }
    });
  }

  generatePlan(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    this.plannerApi
      .plan({
        projectId: raw.projectId,
        objective: raw.objective.trim(),
        runName: raw.runName.trim() || null,
        templateId: raw.templateId || null,
        metadataJson: raw.metadataJson.trim() || null,
      })
      .subscribe({
        next: (result) => {
          this.plannerResult.set(result);
          this.editablePlanJson.set(JSON.stringify(result.plan, null, 2));
          this.loading.set(false);
          this.resetView();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to generate plan');
        },
      });
  }

  createDraftRun(): void {
    if (!this.permissions.canImport() || this.creating()) {
      return;
    }
    const plan = this.parseEditedPlan();
    if (!plan) {
      return;
    }
    this.creating.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    this.plannerApi
      .importPlan(raw.projectId, raw.runName.trim() || `Plan: ${plan.objective}`.slice(0, 255), plan)
      .subscribe({
        next: (run) => {
          this.creating.set(false);
          void this.router.navigate(['/orchestration-runs', run.id, 'graph']);
        },
        error: (err) => {
          this.creating.set(false);
          this.error.set(err?.error?.message ?? 'Failed to create draft run');
        },
      });
  }

  applyEditedPlan(): void {
    const plan = this.parseEditedPlan();
    if (!plan) {
      return;
    }
    const current = this.plannerResult();
    if (!current) {
      return;
    }
    this.plannerResult.set({
      ...current,
      plan,
      validated: true,
    });
  }

  zoomIn(): void {
    this.zoom.update((z) => Math.min(2.5, z + 0.1));
  }

  zoomOut(): void {
    this.zoom.update((z) => Math.max(0.5, z - 0.1));
  }

  resetView(): void {
    this.panX.set(0);
    this.panY.set(0);
    this.zoom.set(1);
  }

  onPan(event: WheelEvent): void {
    if (event.shiftKey) {
      event.preventDefault();
      this.panX.update((x) => x - event.deltaY * 0.4);
      this.panY.update((y) => y - event.deltaX * 0.4);
    }
  }

  edgePath(dep: ExecutionDependency): string {
    const nodes = this.dagNodes();
    const from = nodes.find((n) => n.task.taskKey === dep.from);
    const to = nodes.find((n) => n.task.taskKey === dep.to);
    if (!from || !to) {
      return '';
    }
    const x1 = from.x + 90;
    const y1 = from.y + 28;
    const x2 = to.x;
    const y2 = to.y + 28;
    const mid = (x1 + x2) / 2;
    return `M ${x1} ${y1} C ${mid} ${y1}, ${mid} ${y2}, ${x2} ${y2}`;
  }

  private currentPlan(): ExecutionPlan | null {
    return this.plannerResult()?.plan ?? null;
  }

  private parseEditedPlan(): ExecutionPlan | null {
    try {
      const plan = JSON.parse(this.editablePlanJson()) as ExecutionPlan;
      if (!plan?.tasks?.length) {
        this.error.set('Edited plan must include tasks');
        return null;
      }
      this.error.set(null);
      return plan;
    } catch {
      this.error.set('Edited plan JSON is invalid');
      return null;
    }
  }

  private loadTemplates(projectId: string): void {
    if (!this.permissions.canReadTemplates()) {
      return;
    }
    this.plannerApi.listTemplates(projectId).subscribe({
      next: (templates) => this.templates.set(templates),
      error: () => this.templates.set([]),
    });
  }

  private layoutNodes(tasks: ExecutionTaskDefinition[]): DagNode[] {
    const cols = Math.max(1, Math.ceil(Math.sqrt(tasks.length || 1)));
    return tasks.map((task, index) => ({
      task,
      x: 40 + (index % cols) * 220,
      y: 40 + Math.floor(index / cols) * 110,
    }));
  }
}
