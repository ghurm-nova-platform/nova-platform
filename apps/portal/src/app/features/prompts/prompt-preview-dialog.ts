import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PromptPreviewResponse, PromptVariableRequest } from './prompt.models';
import { PromptService } from './prompt.service';

export interface PromptPreviewDialogData {
  projectId: string;
  content: string;
  variables: PromptVariableRequest[];
}

@Component({
  selector: 'app-prompt-preview-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Preview prompt</h2>
    <mat-dialog-content>
      <p class="hint">Preview values are kept in memory only for this dialog.</p>
      <form class="preview-form" [formGroup]="valuesForm">
        @for (variable of data.variables; track variable.name) {
          <mat-form-field appearance="outline">
            <mat-label>{{ variable.name }}</mat-label>
            <input matInput [formControlName]="variable.name" />
          </mat-form-field>
        }
      </form>

      @if (loading()) {
        <div class="preview-loading"><mat-spinner diameter="28" /></div>
      } @else if (error()) {
        <p class="preview-error" role="alert">{{ error() }}</p>
      } @else if (result(); as preview) {
        @if (preview.errors.length > 0) {
          <ul class="preview-messages preview-messages--error">
            @for (message of preview.errors; track message) {
              <li>{{ message }}</li>
            }
          </ul>
        }
        @if (preview.warnings.length > 0) {
          <ul class="preview-messages preview-messages--warn">
            @for (message of preview.warnings; track message) {
              <li>{{ message }}</li>
            }
          </ul>
        }
        @if (preview.missingRequiredVariables.length > 0) {
          <p class="preview-error">
            Missing required variables: {{ preview.missingRequiredVariables.join(', ') }}
          </p>
        }
        <pre class="preview-output">{{ preview.renderedContent }}</pre>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Close</button>
      <button mat-flat-button color="primary" type="button" (click)="runPreview()" [disabled]="loading()">
        Render preview
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .hint {
      margin: 0 0 0.75rem;
      opacity: 0.8;
      font-size: 0.875rem;
    }

    .preview-form {
      display: grid;
      gap: 0.5rem;
    }

    .preview-loading {
      display: grid;
      place-items: center;
      min-height: 80px;
    }

    .preview-error {
      color: #b3261e;
    }

    .preview-messages {
      margin: 0.5rem 0;
      padding-left: 1.25rem;
    }

    .preview-messages--error {
      color: #b3261e;
    }

    .preview-messages--warn {
      color: #8a6d00;
    }

    .preview-output {
      white-space: pre-wrap;
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 0.875rem;
      background: color-mix(in srgb, currentColor 6%, transparent);
      padding: 0.75rem;
      border-radius: 0.5rem;
      margin: 0.75rem 0 0;
    }
  `,
})
export class PromptPreviewDialog {
  private readonly fb = inject(FormBuilder);
  private readonly promptsApi = inject(PromptService);
  private readonly dialogRef = inject(MatDialogRef<PromptPreviewDialog>);
  readonly data = inject<PromptPreviewDialogData>(MAT_DIALOG_DATA);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<PromptPreviewResponse | null>(null);

  readonly valuesForm = this.fb.group<Record<string, string>>({});

  constructor() {
    for (const variable of this.data.variables) {
      this.valuesForm.addControl(
        variable.name,
        this.fb.nonNullable.control(variable.sampleValue ?? variable.defaultValue ?? ''),
      );
    }
    this.dialogRef.afterClosed().subscribe(() => {
      this.valuesForm.reset();
      this.result.set(null);
    });
  }

  runPreview(): void {
    this.loading.set(true);
    this.error.set(null);
    const values: Record<string, string> = {};
    for (const [key, control] of Object.entries(this.valuesForm.controls)) {
      values[key] = control.value ?? '';
    }
    this.promptsApi
      .preview(this.data.projectId, {
        content: this.data.content,
        variables: this.data.variables,
        values,
      })
      .subscribe({
        next: (response) => {
          this.result.set(response);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Unable to render preview.');
          this.loading.set(false);
        },
      });
  }
}
