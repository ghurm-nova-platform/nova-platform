import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

import { IdentityPermissionHelper } from './identity-permission.helper';
import { identityNavItems } from './identity.routes';

@Component({
  selector: 'app-identity-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatListModule],
  templateUrl: './identity-page.html',
  styleUrl: './identity-page.scss',
})
export class IdentityPage implements OnInit {
  readonly permissions = inject(IdentityPermissionHelper);
  readonly navItems = identityNavItems;
  readonly unauthorized = signal(false);

  ngOnInit(): void {
    if (!this.permissions.canRead()) {
      this.unauthorized.set(true);
    }
  }
}
