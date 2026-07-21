import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { identityRoutes } from './identity.routes';

@Component({ template: 'identity shell' })
class IdentityShellStubComponent {}

@Component({ template: 'users section' })
class IdentityUsersStubComponent {}

describe('IdentityRoutingTest', () => {
  it('loads /identity route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'identity',
            component: IdentityShellStubComponent,
            children: identityRoutes,
          },
        ]),
      ],
    });
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
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/identity/users');
    expect(TestBed.inject(Router).url).toBe('/identity/users');
  });
});
