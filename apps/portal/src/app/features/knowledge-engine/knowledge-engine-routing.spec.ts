import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

@Component({ template: 'knowledge engine stub' })
class KnowledgeEngineStubComponent {}

describe('KnowledgeRoutingTest', () => {
  it('loads /knowledge route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'knowledge', component: KnowledgeEngineStubComponent }]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/knowledge');
    expect(TestBed.inject(Router).url).toBe('/knowledge');
  });
});
