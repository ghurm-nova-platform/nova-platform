import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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

import { AuthService } from '../../auth/services/auth.service';
import { UserSessionService } from '../../auth/services/user-session.service';
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
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly session = inject(UserSessionService);
  readonly direction = inject(DirectionService);
  readonly theme = inject(ThemeService);

  readonly navOpen = signal(true);
  readonly signingOut = signal(false);

  readonly isHandset = toSignal(
    this.breakpointObserver
      .observe(Breakpoints.Handset)
      .pipe(map((result) => result.matches)),
    { initialValue: false },
  );

  readonly navItems: NavItem[] = [
    { labelEn: 'Dashboard', labelAr: 'لوحة التحكم', path: '/dashboard', icon: 'dashboard' },
    { labelEn: 'Organizations', labelAr: 'المنظمات', path: '/organizations', icon: 'business' },
    { labelEn: 'Projects', labelAr: 'المشاريع', path: '/projects', icon: 'folder' },
    { labelEn: 'Model providers', labelAr: 'مزودو النماذج', path: '/model-providers', icon: 'hub' },
    { labelEn: 'AI Models', labelAr: 'نماذج الذكاء', path: '/ai-models', icon: 'model_training' },
    { labelEn: 'Provider secrets', labelAr: 'أسرار المزودين', path: '/provider-secrets', icon: 'vpn_key' },
    {
      labelEn: 'AI Planner',
      labelAr: 'مخطط الذكاء',
      path: '/planner',
      icon: 'schema',
    },
    {
      labelEn: 'Coding Agent',
      labelAr: 'وكيل البرمجة',
      path: '/coding',
      icon: 'code',
    },
    {
      labelEn: 'Review Agent',
      labelAr: 'وكيل المراجعة',
      path: '/review',
      icon: 'rate_review',
    },
    {
      labelEn: 'Testing Agent',
      labelAr: 'وكيل الاختبار',
      path: '/testing',
      icon: 'science',
    },
    {
      labelEn: 'Patch Agent',
      labelAr: 'وكيل التصحيح',
      path: '/patch',
      icon: 'difference',
    },
    {
      labelEn: 'Git Integration',
      labelAr: 'تكامل Git',
      path: '/git',
      icon: 'merge',
    },
    {
      labelEn: 'Pull Request Agent',
      labelAr: 'وكيل طلبات السحب',
      path: '/pull-requests',
      icon: 'call_merge',
    },
    {
      labelEn: 'CI Observation',
      labelAr: 'مراقبة CI',
      path: '/ci',
      icon: 'monitoring',
    },
    {
      labelEn: 'Repair Agent',
      labelAr: 'وكيل الإصلاح',
      path: '/repair',
      icon: 'build',
    },
    {
      labelEn: 'Approval Gate',
      labelAr: 'بوابة الموافقة',
      path: '/approval-gate',
      icon: 'verified_user',
    },
    {
      labelEn: 'Merge Agent',
      labelAr: 'وكيل الدمج',
      path: '/merge',
      icon: 'call_merge',
    },
    {
      labelEn: 'Releases',
      labelAr: 'الإصدارات',
      path: '/releases',
      icon: 'new_releases',
    },
    {
      labelEn: 'Deployments',
      labelAr: 'عمليات النشر',
      path: '/deployments',
      icon: 'cloud_done',
    },
    {
      labelEn: 'Rollbacks',
      labelAr: 'التراجع',
      path: '/rollbacks',
      icon: 'undo',
    },
    {
      labelEn: 'AI Orchestration',
      labelAr: 'تنسيق الوكلاء',
      path: '/orchestration-runs',
      icon: 'account_tree',
    },
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

  signOut(): void {
    if (this.signingOut()) {
      return;
    }
    this.signingOut.set(true);
    this.auth.logout().subscribe({
      next: () => {
        this.signingOut.set(false);
        void this.router.navigateByUrl('/login');
      },
      error: () => {
        this.signingOut.set(false);
        void this.router.navigateByUrl('/login');
      },
    });
  }
}
