import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { ApiTokenView } from '../identity.models';
import { extractErrorMessage } from '../identity.utils';

@Component({
  selector: 'app-identity-api-tokens-page',
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
  templateUrl: './identity-api-tokens-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityApiTokensPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly tokens = signal<ApiTokenView[]>([]);
  readonly createdSecret = signal<string | null>(null);
  readonly showCreate = signal(false);

  newName = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listApiTokens().subscribe({
      next: (items) => {
        this.tokens.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to load API tokens'));
        this.tokens.set([]);
      },
    });
  }

  startCreate(): void {
    if (!this.permissions.canManageApiTokens()) {
      return;
    }
    this.showCreate.set(true);
    this.newName = '';
    this.createdSecret.set(null);
  }

  createToken(): void {
    if (!this.permissions.canManageApiTokens() || !this.newName.trim()) {
      return;
    }
    this.loading.set(true);
    this.api.createApiToken({ name: this.newName.trim() }).subscribe({
      next: (response) => {
        this.showCreate.set(false);
        this.createdSecret.set(response.secret);
        this.message.set('API token created. Copy the secret now — it will not be shown again.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to create API token'));
      },
    });
  }

  revokeToken(id: string): void {
    if (!this.permissions.canManageApiTokens()) {
      return;
    }
    this.loading.set(true);
    this.api.revokeApiToken(id).subscribe({
      next: () => {
        this.message.set('API token revoked.');
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'Failed to revoke API token'));
      },
    });
  }
}
