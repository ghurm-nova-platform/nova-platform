import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

@Component({ template: 'llm stub' })
class LlmStubComponent {}

describe('LlmRoutingTest', () => {
  it('loads /llm route', async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'llm', component: LlmStubComponent }])],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/llm');
    expect(TestBed.inject(Router).url).toBe('/llm');
  });
});
