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
import { ServiceAccountView } from '../identity.models';
import { extractErrorMessage } from '../identity.utils';

@Component({
  selector: 'app-identity-service-accounts-page',
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
  templateUrl: './identity-service-accounts-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityServiceAccountsPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly accounts = signal<ServiceAccountView[]>([]);
  readonly showCreate = signal(false);

  newName = '';
  newDescription = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listServiceAccounts().subscribe({
      next: (items) => {
        this.accounts.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load service accounts'));
        this.accounts.set([]);
      },
    });
  }

  startCreate(): void {
    if (!this.permissions.canManageServiceAccounts()) {
      return;
    }
    this.showCreate.set(true);
    this.newName = '';
    this.newDescription = '';
  }

  createAccount(): void {
    if (!this.permissions.canManageServiceAccounts() || !this.newName.trim()) {
      return;
    }
    this.loading.set(true);
    this.api
      .createServiceAccount({
        name: this.newName.trim(),
        description: this.newDescription.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.showCreate.set(false);
          this.message.set('Service account created.');
          this.reload();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to create service account'));
        },
      });
  }

  deleteAccount(id: string): void {
    if (!this.permissions.canManageServiceAccounts()) {
      return;
    }
    this.loading.set(true);
    this.api.deleteServiceAccount(id).subscribe({
      next: () => {
        this.message.set('Service account deleted.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to delete service account'));
      },
    });
  }
}
