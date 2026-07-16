import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { DirectionService } from '../../core/services/direction.service';
import { ThemeService } from '../../core/services/theme.service';

interface NavItem {
  labelEn: string;
  labelAr: string;
  path: string;
  icon: string;
}

/**
 * Application shell with toolbar, collapsible sidenav, content area, and user menu.
 */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly breakpointObserver = inject(BreakpointObserver);
  readonly direction = inject(DirectionService);
  readonly theme = inject(ThemeService);

  readonly navOpen = signal(true);

  readonly isHandset = toSignal(
    this.breakpointObserver
      .observe(Breakpoints.Handset)
      .pipe(map((result) => result.matches)),
    { initialValue: false },
  );

  readonly navItems: NavItem[] = [
    { labelEn: 'Dashboard', labelAr: 'لوحة التحكم', path: '/dashboard', icon: 'dashboard' },
    { labelEn: 'Projects', labelAr: 'المشاريع', path: '/projects', icon: 'folder' },
    { labelEn: 'Agents', labelAr: 'الوكلاء', path: '/agents', icon: 'smart_toy' },
    { labelEn: 'Feedback', labelAr: 'الملاحظات', path: '/feedback', icon: 'feedback' },
    { labelEn: 'Settings', labelAr: 'الإعدادات', path: '/settings', icon: 'settings' },
  ];

  toggleNav(): void {
    this.navOpen.update((open) => !open);
  }

  labelFor(item: NavItem): string {
    return this.direction.locale() === 'ar' ? item.labelAr : item.labelEn;
  }
}
