import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';

import { Project, ProjectRequest, ProjectStatus, ProjectVisibility } from '../../core/models/catalog';

export interface ProjectDialogData {
  mode: 'create' | 'edit';
  project?: Project;
}

@Component({
  selector: 'app-project-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Create project' : 'Edit project' }}</h2>
    <mat-dialog-content>
      <form class="dialog-form" [formGroup]="form">
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" required />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput rows="3" formControlName="description"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            @for (status of statuses; track status) {
              <mat-option [value]="status">{{ status }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Visibility</mat-label>
          <mat-select formControlName="visibility">
            @for (visibility of visibilities; track visibility) {
              <mat-option [value]="visibility">{{ visibility }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">
        Save
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog-form {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      min-width: min(100%, 420px);
      padding-top: 0.5rem;
    }
  `,
})
export class ProjectDialog {
  readonly data = inject<ProjectDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ProjectDialog, ProjectRequest>);
  private readonly fb = inject(FormBuilder);

  readonly statuses: ProjectStatus[] = ['ACTIVE', 'DRAFT', 'ARCHIVED'];
  readonly visibilities: ProjectVisibility[] = ['PRIVATE', 'INTERNAL', 'PUBLIC'];

  readonly form = this.fb.nonNullable.group({
    name: [this.data.project?.name ?? '', [Validators.required, Validators.maxLength(255)]],
    description: [this.data.project?.description ?? ''],
    status: this.fb.nonNullable.control<ProjectStatus>(this.data.project?.status ?? 'ACTIVE'),
    visibility: this.fb.nonNullable.control<ProjectVisibility>(
      this.data.project?.visibility ?? 'PRIVATE',
    ),
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.dialogRef.close({
      name: raw.name.trim(),
      description: raw.description.trim() || null,
      status: raw.status,
      visibility: raw.visibility,
    });
  }
}
