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
        path: 'organizations',
        loadComponent: () =>
          import('./features/organizations/organizations-page').then((m) => m.OrganizationsPage),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/projects/projects-page').then((m) => m.ProjectsPage),
      },
      {
        path: 'projects/:projectId/agents',
        loadComponent: () =>
          import('./features/agents/agents-list-page').then((m) => m.AgentsListPage),
      },
      {
        path: 'projects/:projectId/agents/new',
        loadComponent: () =>
          import('./features/agents/agent-form-page').then((m) => m.AgentFormPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId',
        loadComponent: () =>
          import('./features/agents/agent-detail-page').then((m) => m.AgentDetailPage),
      },
      {
        path: 'projects/:projectId/agents/:agentId/edit',
        loadComponent: () =>
          import('./features/agents/agent-form-page').then((m) => m.AgentFormPage),
      },
      {
        path: 'agents',
        redirectTo: 'projects',
        pathMatch: 'full',
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
