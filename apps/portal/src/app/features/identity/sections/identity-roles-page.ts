import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { IdentityRoleView } from '../identity.models';
import { extractErrorMessage } from '../identity.utils';

@Component({
  selector: 'app-identity-roles-page',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './identity-roles-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityRolesPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly roles = signal<IdentityRoleView[]>([]);
  readonly showCreate = signal(false);

  newCode = '';
  newName = '';
  newDescription = '';
  cloneSourceId: string | null = null;
  cloneCode = '';
  cloneName = '';
  permissionInput = '';
  assignRoleId: string | null = null;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listRoles().subscribe({
      next: (items) => {
        this.roles.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load roles'));
        this.roles.set([]);
      },
    });
  }

  startCreate(): void {
    if (!this.permissions.canManageRoles()) {
      return;
    }
    this.showCreate.set(true);
    this.newCode = '';
    this.newName = '';
    this.newDescription = '';
  }

  createRole(): void {
    if (!this.permissions.canManageRoles() || !this.newCode.trim() || !this.newName.trim()) {
      return;
    }
    this.loading.set(true);
    this.api
      .createRole({
        code: this.newCode.trim(),
        name: this.newName.trim(),
        description: this.newDescription.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.showCreate.set(false);
          this.message.set('Role created.');
          this.reload();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to create role'));
        },
      });
  }

  deleteRole(id: string): void {
    if (!this.permissions.canManageRoles()) {
      return;
    }
    this.loading.set(true);
    this.api.deleteRole(id).subscribe({
      next: () => {
        this.message.set('Role deleted.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to delete role'));
      },
    });
  }

  startClone(role: IdentityRoleView): void {
    this.cloneSourceId = role.id;
    this.cloneCode = `${role.code}_COPY`;
    this.cloneName = `${role.name} Copy`;
  }

  cloneRole(): void {
    if (!this.permissions.canManageRoles() || !this.cloneSourceId) {
      return;
    }
    this.api
      .cloneRole(this.cloneSourceId, { code: this.cloneCode.trim(), name: this.cloneName.trim() })
      .subscribe({
        next: () => {
          this.cloneSourceId = null;
          this.message.set('Role cloned.');
          this.reload();
        },
        error: (err) => this.error.set(extractErrorMessage(err, 'Failed to clone role')),
      });
  }

  assignPermissions(): void {
    if (!this.permissions.canManageRoles() || !this.assignRoleId || !this.permissionInput.trim()) {
      return;
    }
    const codes = this.permissionInput
      .split(',')
      .map((code) => code.trim())
      .filter(Boolean);
    this.api.assignRolePermissions(this.assignRoleId, { permissionCodes: codes }).subscribe({
      next: () => {
        this.permissionInput = '';
        this.message.set('Permissions assigned.');
        this.reload();
      },
      error: (err) => this.error.set(extractErrorMessage(err, 'Failed to assign permissions')),
    });
  }
}
