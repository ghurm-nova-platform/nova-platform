import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { catchError, of } from 'rxjs';

import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { IdentityGroupView } from '../identity.models';
import { extractErrorMessage } from '../identity.utils';

@Component({
  selector: 'app-identity-groups-page',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './identity-groups-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityGroupsPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly groups = signal<IdentityGroupView[]>([]);
  readonly showCreate = signal(false);

  newName = '';
  newDescription = '';
  editId: string | null = null;
  editName = '';
  editDescription = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .listGroups()
      .pipe(catchError(() => this.api.getSummary().pipe(catchError(() => of(null)))))
      .subscribe({
        next: (result) => {
          if (Array.isArray(result)) {
            this.groups.set(result);
          } else if (result?.groups) {
            this.groups.set(result.groups);
          } else {
            this.groups.set([]);
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to load groups'));
          this.groups.set([]);
        },
      });
  }

  startCreate(): void {
    if (!this.permissions.canManageGroups()) {
      return;
    }
    this.showCreate.set(true);
    this.newName = '';
    this.newDescription = '';
  }

  createGroup(): void {
    if (!this.permissions.canManageGroups() || !this.newName.trim()) {
      return;
    }
    this.loading.set(true);
    this.api.createGroup({ name: this.newName.trim(), description: this.newDescription.trim() }).subscribe({
      next: () => {
        this.showCreate.set(false);
        this.message.set('Group created.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to create group'));
      },
    });
  }

  startEdit(group: IdentityGroupView): void {
    this.editId = group.id;
    this.editName = group.name;
    this.editDescription = group.description ?? '';
  }

  saveEdit(): void {
    if (!this.permissions.canManageGroups() || !this.editId) {
      return;
    }
    this.loading.set(true);
    this.api
      .updateGroup(this.editId, {
        name: this.editName.trim(),
        description: this.editDescription.trim(),
      })
      .subscribe({
        next: () => {
          this.editId = null;
          this.message.set('Group updated.');
          this.reload();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to update group'));
        },
      });
  }

  deleteGroup(id: string): void {
    if (!this.permissions.canManageGroups()) {
      return;
    }
    this.loading.set(true);
    this.api.deleteGroup(id).subscribe({
      next: () => {
        this.message.set('Group deleted.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to delete group'));
      },
    });
  }

  syncGroup(id: string): void {
    if (!this.permissions.canManageGroups()) {
      return;
    }
    this.api.syncGroup(id).subscribe({
      next: () => this.message.set('Group sync completed.'),
      error: (err) => this.error.set(extractErrorMessage(err, 'Group sync failed')),
    });
  }
}
