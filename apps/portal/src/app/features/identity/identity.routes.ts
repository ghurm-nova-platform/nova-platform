import { Routes } from '@angular/router';

import { identityPermissionGuard } from './identity-permission.guard';

export const identityRoutes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-dashboard-page').then((m) => m.IdentityDashboardPage),
  },
  {
    path: 'providers',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-providers-page').then((m) => m.IdentityProvidersPage),
  },
  {
    path: 'users',
    canActivate: [identityPermissionGuard],
    loadComponent: () => import('./sections/identity-users-page').then((m) => m.IdentityUsersPage),
  },
  {
    path: 'groups',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-groups-page').then((m) => m.IdentityGroupsPage),
  },
  {
    path: 'roles',
    canActivate: [identityPermissionGuard],
    loadComponent: () => import('./sections/identity-roles-page').then((m) => m.IdentityRolesPage),
  },
  {
    path: 'permissions',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-permissions-page').then((m) => m.IdentityPermissionsPage),
  },
  {
    path: 'sessions',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-sessions-page').then((m) => m.IdentitySessionsPage),
  },
  {
    path: 'api-tokens',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-api-tokens-page').then((m) => m.IdentityApiTokensPage),
  },
  {
    path: 'service-accounts',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-service-accounts-page').then((m) => m.IdentityServiceAccountsPage),
  },
  {
    path: 'audit',
    canActivate: [identityPermissionGuard],
    loadComponent: () => import('./sections/identity-audit-page').then((m) => m.IdentityAuditPage),
  },
  {
    path: 'security-events',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-security-events-page').then((m) => m.IdentitySecurityEventsPage),
  },
  {
    path: 'configuration',
    canActivate: [identityPermissionGuard],
    loadComponent: () =>
      import('./sections/identity-configuration-page').then((m) => m.IdentityConfigurationPage),
  },
];

export const identityNavItems = [
  { path: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
  { path: 'providers', label: 'Identity Providers', icon: 'vpn_key' },
  { path: 'users', label: 'Users', icon: 'people' },
  { path: 'groups', label: 'Groups', icon: 'groups' },
  { path: 'roles', label: 'Roles', icon: 'admin_panel_settings' },
  { path: 'permissions', label: 'Permissions', icon: 'lock' },
  { path: 'sessions', label: 'Sessions', icon: 'devices' },
  { path: 'api-tokens', label: 'API Tokens', icon: 'token' },
  { path: 'service-accounts', label: 'Service Accounts', icon: 'smart_toy' },
  { path: 'audit', label: 'Audit', icon: 'fact_check' },
  { path: 'security-events', label: 'Security Events', icon: 'shield' },
  { path: 'configuration', label: 'Configuration', icon: 'settings' },
] as const;
