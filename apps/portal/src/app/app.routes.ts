import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './auth/guards/auth.guard';
import { Shell } from './layout/shell/shell';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/projects/projects-page').then((m) => m.ProjectsPage),
      },
      {
        path: 'agents',
        loadComponent: () => import('./features/agents/agents-page').then((m) => m.AgentsPage),
      },
      {
        path: 'feedback',
        loadComponent: () =>
          import('./features/feedback/feedback-page').then((m) => m.FeedbackPage),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings-page').then((m) => m.SettingsPage),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
