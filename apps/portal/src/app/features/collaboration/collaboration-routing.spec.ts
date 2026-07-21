import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

@Component({ template: 'collaboration stub' })
class CollaborationStubComponent {}

describe('CollaborationRoutingTest', () => {
  it('loads /collaboration route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'collaboration', component: CollaborationStubComponent }]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/collaboration');
    expect(TestBed.inject(Router).url).toBe('/collaboration');
  });
});
