import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AuthUser } from '../../auth/services/auth.models';
import { UserSessionService } from '../../auth/services/user-session.service';
import { identityRoutes } from './identity.routes';

@Component({ template: 'identity shell' })
class IdentityShellStubComponent {}

@Component({ template: 'users section' })
class IdentityUsersStubComponent {}

@Component({ template: 'groups section' })
class IdentityGroupsStubComponent {}

@Component({ template: 'roles section' })
class IdentityRolesStubComponent {}

describe('IdentityRoutingTest', () => {
  const admin: AuthUser = {
    userId: '44444444-4444-4444-4444-444444444401',
    organizationId: '11111111-1111-1111-1111-111111111111',
    email: 'admin@nova.local',
    displayName: 'Nova Admin',
    roles: ['ORG_ADMIN'],
    permissions: [],
  };

  afterEach(() => {
    TestBed.inject(UserSessionService).clear();
  });

  function grantIdentityAccess(): void {
    TestBed.inject(UserSessionService).setSession(
      { accessToken: 'token', refreshToken: 'refresh' },
      admin,
    );
  }

  it('loads /identity route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'identity',
            component: IdentityShellStubComponent,
            children: identityRoutes.map((route) =>
              route.path === 'dashboard'
                ? { path: 'dashboard', component: IdentityShellStubComponent }
                : route,
            ),
          },
          { path: 'dashboard', component: IdentityShellStubComponent },
        ]),
      ],
    });
    grantIdentityAccess();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/identity');
    expect(TestBed.inject(Router).url).toBe('/identity/dashboard');
  });

  it('loads /identity/users child route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'identity',
            component: IdentityShellStubComponent,
            children: [
              ...identityRoutes.filter((route) => route.path !== 'users'),
              { path: 'users', component: IdentityUsersStubComponent },
            ],
          },
          { path: 'dashboard', component: IdentityShellStubComponent },
        ]),
      ],
    });
    grantIdentityAccess();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/identity/users');
    expect(TestBed.inject(Router).url).toBe('/identity/users');
  });

  it('loads /identity/groups child route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'identity',
            component: IdentityShellStubComponent,
            children: [
              ...identityRoutes.filter((route) => route.path !== 'groups'),
              { path: 'groups', component: IdentityGroupsStubComponent },
            ],
          },
          { path: 'dashboard', component: IdentityShellStubComponent },
        ]),
      ],
    });
    grantIdentityAccess();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/identity/groups');
    expect(TestBed.inject(Router).url).toBe('/identity/groups');
  });

  it('loads /identity/roles child route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'identity',
            component: IdentityShellStubComponent,
            children: [
              ...identityRoutes.filter((route) => route.path !== 'roles'),
              { path: 'roles', component: IdentityRolesStubComponent },
            ],
          },
          { path: 'dashboard', component: IdentityShellStubComponent },
        ]),
      ],
    });
    grantIdentityAccess();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/identity/roles');
    expect(TestBed.inject(Router).url).toBe('/identity/roles');
  });

  it('redirects unauthorized users away from identity routes', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'identity',
            component: IdentityShellStubComponent,
            children: [
              { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
              {
                path: 'dashboard',
                canActivate: identityRoutes.find((route) => route.path === 'dashboard')?.canActivate,
                component: IdentityShellStubComponent,
              },
            ],
          },
          { path: 'dashboard', component: IdentityShellStubComponent },
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/identity/dashboard');
    expect(TestBed.inject(Router).url).toBe('/dashboard');
  });
});
