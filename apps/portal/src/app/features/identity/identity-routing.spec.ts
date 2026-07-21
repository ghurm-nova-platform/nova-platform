import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

@Component({ template: 'identity stub' })
class IdentityStubComponent {}

describe('IdentityRoutingTest', () => {
  it('loads /identity route', async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'identity', component: IdentityStubComponent }])],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/identity');
    expect(TestBed.inject(Router).url).toBe('/identity');
  });
});
