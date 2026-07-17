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

import { ToolDefinition } from './tool.models';
import { ToolPermissionHelper } from './tool-permission.helper';
import { ToolService } from './tool.service';

@Component({
  selector: 'app-tool-form-page',
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
  templateUrl: './tool-form-page.html',
  styleUrl: './tool-form-page.scss',
})
export class ToolFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toolsApi = inject(ToolService);
  readonly permissions = inject(ToolPermissionHelper);

  readonly projectId = signal('');
  readonly toolId = signal<string | null>(null);
  readonly editMode = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly unauthorized = signal(false);
  readonly executorKeys = signal<string[]>([]);
  readonly currentTool = signal<ToolDefinition | null>(null);

  readonly form = this.fb.nonNullable.group({
    toolKey: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/), Validators.maxLength(100)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', Validators.maxLength(2000)],
    executorKey: ['', Validators.required],
    inputSchema: ['{}', Validators.required],
    outputSchema: [''],
    requiresApproval: [false],
    maxExecutionSeconds: [30, [Validators.required, Validators.min(1), Validators.max(30)]],
    maxOutputCharacters: [10000, [Validators.required, Validators.min(1), Validators.max(50000)]],
  });

  ngOnInit(): void {
    this.projectId.set(this.route.snapshot.paramMap.get('projectId') ?? '');
    const toolId = this.route.snapshot.paramMap.get('toolId');
    this.toolId.set(toolId);
    this.editMode.set(!!toolId);

    if (toolId) {
      if (!this.permissions.canUpdate()) {
        this.unauthorized.set(true);
        return;
      }
      this.form.controls.toolKey.disable();
      this.loadTool(toolId);
    } else if (!this.permissions.canCreate()) {
      this.unauthorized.set(true);
    }

    this.loadExecutors();
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.validateJsonFields()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    const outputSchema = raw.outputSchema.trim() || null;

    if (this.editMode() && this.toolId()) {
      const current = this.currentTool();
      if (!current) {
        return;
      }
      this.toolsApi
        .updateTool(this.projectId(), this.toolId()!, {
          version: current.version,
          name: raw.name.trim(),
          description: raw.description.trim() || null,
          executorKey: raw.executorKey,
          inputSchema: raw.inputSchema.trim(),
          outputSchema,
          requiresApproval: raw.requiresApproval,
          maxExecutionSeconds: raw.maxExecutionSeconds,
          maxOutputCharacters: raw.maxOutputCharacters,
        })
        .subscribe({
          next: (tool) => {
            this.saving.set(false);
            this.currentTool.set(tool);
            void this.router.navigate(['/projects', this.projectId(), 'tools', tool.id]);
          },
          error: (err: { status?: number; error?: { message?: string } }) => {
            this.saving.set(false);
            if (err.status === 403) {
              this.unauthorized.set(true);
              return;
            }
            this.error.set(err.error?.message ?? 'Unable to save tool.');
          },
        });
      return;
    }

    this.toolsApi
      .createTool(this.projectId(), {
        toolKey: raw.toolKey.trim(),
        name: raw.name.trim(),
        description: raw.description.trim() || null,
        executorKey: raw.executorKey,
        inputSchema: raw.inputSchema.trim(),
        outputSchema,
        requiresApproval: raw.requiresApproval,
        maxExecutionSeconds: raw.maxExecutionSeconds,
        maxOutputCharacters: raw.maxOutputCharacters,
      })
      .subscribe({
        next: (tool) => {
          this.saving.set(false);
          void this.router.navigate(['/projects', this.projectId(), 'tools', tool.id]);
        },
        error: (err: { status?: number; error?: { message?: string } }) => {
          this.saving.set(false);
          if (err.status === 403) {
            this.unauthorized.set(true);
            return;
          }
          this.error.set(err.error?.message ?? 'Unable to create tool.');
        },
      });
  }

  activate(): void {
    const current = this.currentTool();
    if (!current || !this.permissions.canActivate() || !window.confirm(`Activate "${current.name}"?`)) {
      return;
    }
    this.toolsApi.activateTool(this.projectId(), current.id).subscribe({
      next: (tool) => this.currentTool.set(tool),
      error: () => this.error.set('Unable to activate tool.'),
    });
  }

  archive(): void {
    const current = this.currentTool();
    if (!current || !this.permissions.canArchive() || !window.confirm(`Archive "${current.name}"?`)) {
      return;
    }
    this.toolsApi.archiveTool(this.projectId(), current.id).subscribe({
      next: () => void this.router.navigate(['/projects', this.projectId(), 'tools']),
      error: () => this.error.set('Unable to archive tool.'),
    });
  }

  statusClass(status: ToolDefinition['status']): string {
    return `status status--${status.toLowerCase()}`;
  }

  private loadTool(toolId: string): void {
    this.loading.set(true);
    this.toolsApi.getTool(this.projectId(), toolId).subscribe({
      next: (tool) => {
        this.currentTool.set(tool);
        this.form.patchValue({
          toolKey: tool.toolKey,
          name: tool.name,
          description: tool.description ?? '',
          executorKey: tool.executorKey,
          inputSchema: tool.inputSchema,
          outputSchema: tool.outputSchema ?? '',
          requiresApproval: tool.requiresApproval,
          maxExecutionSeconds: tool.maxExecutionSeconds,
          maxOutputCharacters: tool.maxOutputCharacters,
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load tool.');
        this.loading.set(false);
      },
    });
  }

  private loadExecutors(): void {
    if (!this.permissions.canRead()) {
      return;
    }
    this.toolsApi.listExecutors(this.projectId()).subscribe({
      next: (response) => this.executorKeys.set(response.executorKeys),
      error: () => this.error.set('Unable to load executor allowlist.'),
    });
  }

  private validateJsonFields(): boolean {
    try {
      JSON.parse(this.form.controls.inputSchema.value.trim());
    } catch {
      this.error.set('Input schema must be valid JSON.');
      return false;
    }
    const output = this.form.controls.outputSchema.value.trim();
    if (output) {
      try {
        JSON.parse(output);
      } catch {
        this.error.set('Output schema must be valid JSON.');
        return false;
      }
    }
    return true;
  }
}
