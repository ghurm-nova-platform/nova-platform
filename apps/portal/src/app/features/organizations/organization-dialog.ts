import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

import { Organization, OrganizationRequest } from '../../core/models/catalog';

export interface OrganizationDialogData {
  mode: 'create' | 'edit';
  organization?: Organization;
}

@Component({
  selector: 'app-organization-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Create organization' : 'Edit organization' }}</h2>
    <mat-dialog-content>
      <form class="dialog-form" [formGroup]="form">
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" required />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Slug</mat-label>
          <input matInput formControlName="slug" />
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
      min-width: min(100%, 360px);
      padding-top: 0.5rem;
    }
  `,
})
export class OrganizationDialog {
  readonly data = inject<OrganizationDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<OrganizationDialog, OrganizationRequest>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    name: [this.data.organization?.name ?? '', [Validators.required, Validators.maxLength(255)]],
    slug: [this.data.organization?.slug ?? '', [Validators.maxLength(100)]],
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.dialogRef.close({
      name: raw.name.trim(),
      slug: raw.slug.trim() || undefined,
    });
  }
}
