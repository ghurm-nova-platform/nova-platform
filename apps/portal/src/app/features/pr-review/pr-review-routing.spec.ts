import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

@Component({ template: 'pr review stub' })
class PrReviewStubComponent {}

describe('ReviewRoutingTest', () => {
  it('loads /pr-review route', async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'pr-review', component: PrReviewStubComponent }])],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/pr-review');
    expect(TestBed.inject(Router).url).toBe('/pr-review');
  });
});
