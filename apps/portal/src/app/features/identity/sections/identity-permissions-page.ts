import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { IdentityPermissionView } from '../identity.models';
import { extractErrorMessage } from '../identity.utils';

@Component({
  selector: 'app-identity-permissions-page',
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
  templateUrl: './identity-permissions-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityPermissionsPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly items = signal<IdentityPermissionView[]>([]);
  readonly showCreate = signal(false);
  searchQuery = '';

  newCode = '';
  newName = '';
  newDescription = '';
  newCategory = '';

  readonly filtered = computed(() => {
    const query = this.searchQuery.trim().toLowerCase();
    if (!query) {
      return this.items();
    }
    return this.items().filter(
      (item) =>
        item.code.toLowerCase().includes(query) ||
        item.name.toLowerCase().includes(query) ||
        (item.category ?? '').toLowerCase().includes(query),
    );
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listPermissions().subscribe({
      next: (result) => {
        this.items.set(result);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load permissions'));
        this.items.set([]);
      },
    });
  }

  search(): void {
    if (!this.searchQuery.trim()) {
      this.reload();
      return;
    }
    this.loading.set(true);
    this.api.listPermissions(this.searchQuery.trim()).subscribe({
      next: (result) => {
        this.items.set(result);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Permission search failed'));
      },
    });
  }

  startCreate(): void {
    if (!this.permissions.canManagePermissions()) {
      return;
    }
    this.showCreate.set(true);
    this.newCode = '';
    this.newName = '';
    this.newDescription = '';
    this.newCategory = '';
  }

  createPermission(): void {
    if (!this.permissions.canManagePermissions() || !this.newCode.trim() || !this.newName.trim()) {
      return;
    }
    this.loading.set(true);
    this.api
      .createPermission({
        code: this.newCode.trim(),
        name: this.newName.trim(),
        description: this.newDescription.trim() || undefined,
        category: this.newCategory.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.showCreate.set(false);
          this.message.set('Permission created.');
          this.reload();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to create permission'));
        },
      });
  }

  deletePermission(id: string): void {
    if (!this.permissions.canManagePermissions()) {
      return;
    }
    this.loading.set(true);
    this.api.deletePermission(id).subscribe({
      next: () => {
        this.message.set('Permission deleted.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to delete permission'));
      },
    });
  }
}
