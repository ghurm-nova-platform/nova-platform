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
import { IdentityProviderType, IdentityProviderView } from '../identity.models';
import { extractErrorMessage, providerTypeIcon } from '../identity.utils';

@Component({
  selector: 'app-identity-providers-page',
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
  templateUrl: './identity-providers-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityProvidersPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly providers = signal<IdentityProviderView[]>([]);
  readonly showCreate = signal(false);

  newName = '';
  newDisplayName = '';
  newType: IdentityProviderType = 'OIDC';
  editId: string | null = null;
  editDisplayName = '';

  readonly activeProviders = computed(() =>
    this.providers().filter((provider) => provider.status === 'ACTIVE'),
  );

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listProviders().subscribe({
      next: (items) => {
        this.providers.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load identity providers'));
        this.providers.set([]);
      },
    });
  }

  startCreate(): void {
    if (!this.permissions.canManageProviders()) {
      return;
    }
    this.showCreate.set(true);
    this.newName = '';
    this.newDisplayName = '';
    this.newType = 'OIDC';
  }

  createProvider(): void {
    if (!this.permissions.canManageProviders() || !this.newName.trim()) {
      return;
    }
    this.loading.set(true);
    this.api
      .createProvider({
        name: this.newName.trim(),
        displayName: this.newDisplayName.trim() || this.newName.trim(),
        type: this.newType,
      })
      .subscribe({
        next: () => {
          this.showCreate.set(false);
          this.message.set('Provider created.');
          this.reload();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to create provider'));
        },
      });
  }

  startEdit(provider: IdentityProviderView): void {
    this.editId = provider.id;
    this.editDisplayName = provider.displayName || provider.name;
  }

  saveEdit(): void {
    if (!this.permissions.canManageProviders() || !this.editId) {
      return;
    }
    this.loading.set(true);
    this.api.updateProvider(this.editId, { displayName: this.editDisplayName.trim() }).subscribe({
      next: () => {
        this.editId = null;
        this.message.set('Provider updated.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to update provider'));
      },
    });
  }

  testProvider(id: string): void {
    if (!this.permissions.canManageProviders()) {
      return;
    }
    this.api.testProvider(id).subscribe({
      next: (result) => this.message.set(result.message || 'Provider test completed.'),
      error: (err) => this.error.set(extractErrorMessage(err, 'Provider test failed')),
    });
  }

  syncProvider(id: string): void {
    if (!this.permissions.canManageProviders()) {
      return;
    }
    this.api.syncProvider(id).subscribe({
      next: () => {
        this.message.set('Provider sync completed.');
        this.reload();
      },
      error: (err) => this.error.set(extractErrorMessage(err, 'Provider sync failed')),
    });
  }

  providerTypeIcon(type: string): string {
    return providerTypeIcon(type);
  }
}
