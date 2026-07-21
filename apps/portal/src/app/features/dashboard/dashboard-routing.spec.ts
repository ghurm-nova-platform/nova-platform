import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

@Component({ template: 'dashboard stub' })
class DashboardStubComponent {}

describe('dashboard routing', () => {
  it('loads /dashboard route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'dashboard', component: DashboardStubComponent }]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dashboard');
    expect(TestBed.inject(Router).url).toBe('/dashboard');
  });
});
