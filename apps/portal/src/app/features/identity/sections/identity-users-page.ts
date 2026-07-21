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
import { catchError, of } from 'rxjs';

import { IdentityPermissionHelper } from '../identity-permission.helper';
import { IdentityService } from '../identity.service';
import { IdentityUserView, LoginHistoryEntry } from '../identity.models';
import { extractErrorMessage, outcomeClass } from '../identity.utils';

@Component({
  selector: 'app-identity-users-page',
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
  templateUrl: './identity-users-page.html',
  styleUrl: '../identity-page.scss',
})
export class IdentityUsersPage implements OnInit {
  private readonly api = inject(IdentityService);
  readonly permissions = inject(IdentityPermissionHelper);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly users = signal<IdentityUserView[]>([]);
  readonly selectedHistory = signal<LoginHistoryEntry[]>([]);
  readonly selectedUserId = signal<string | null>(null);
  readonly showCreate = signal(false);

  newEmail = '';
  newDisplayName = '';
  newPassword = '';
  resetPasswordValue = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .listUsers()
      .pipe(catchError(() => this.api.getSummary().pipe(catchError(() => of(null)))))
      .subscribe({
        next: (result) => {
          if (Array.isArray(result)) {
            this.users.set(result);
          } else if (result?.users) {
            this.users.set(result.users);
          } else {
            this.users.set([]);
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to load users'));
          this.users.set([]);
        },
      });
  }

  startCreate(): void {
    if (!this.permissions.canManageUsers()) {
      return;
    }
    this.showCreate.set(true);
    this.newEmail = '';
    this.newDisplayName = '';
    this.newPassword = '';
  }

  createUser(): void {
    if (!this.permissions.canManageUsers() || !this.newEmail.trim()) {
      return;
    }
    this.loading.set(true);
    this.api
      .createUser({
        email: this.newEmail.trim(),
        displayName: this.newDisplayName.trim() || this.newEmail.trim(),
        password: this.newPassword.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.showCreate.set(false);
          this.message.set('User created.');
          this.reload();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(extractErrorMessage(err, 'Failed to create user'));
        },
      });
  }

  disableUser(id: string): void {
    this.runUserAction(() => this.api.disableUser(id), 'User disabled.');
  }

  enableUser(id: string): void {
    this.runUserAction(() => this.api.enableUser(id), 'User enabled.');
  }

  unlockUser(id: string): void {
    this.runUserAction(() => this.api.unlockUser(id), 'User unlocked.');
  }

  resetPassword(id: string): void {
    if (!this.permissions.canManageUsers() || !this.resetPasswordValue.trim()) {
      return;
    }
    this.api.resetPassword(id, { newPassword: this.resetPasswordValue.trim() }).subscribe({
      next: () => {
        this.resetPasswordValue = '';
        this.message.set('Password reset.');
      },
      error: (err) => this.error.set(extractErrorMessage(err, 'Failed to reset password')),
    });
  }

  viewLoginHistory(user: IdentityUserView): void {
    this.selectedUserId.set(user.id);
    this.api.getUserLoginHistory(user.id).pipe(catchError(() => this.api.loginHistory())).subscribe({
      next: (entries) => {
        const filtered = entries.filter((entry) => entry.userId === user.id);
        this.selectedHistory.set(filtered.length > 0 ? filtered : entries.slice(0, 20));
      },
      error: () => this.selectedHistory.set([]),
    });
  }

  outcomeClass(outcome: string): string {
    return outcomeClass(outcome);
  }

  private runUserAction(action: () => ReturnType<IdentityService['disableUser']>, success: string): void {
    if (!this.permissions.canManageUsers()) {
      return;
    }
    this.loading.set(true);
    action().subscribe({
      next: () => {
        this.message.set(success);
        this.reload();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(extractErrorMessage(err, 'User action failed'));
      },
    });
  }
}
