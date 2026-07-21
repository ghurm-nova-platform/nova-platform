import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentityProvidersPage } from './identity-providers-page';
import { IdentityService } from '../identity.service';

describe('ProvidersComponentTest', () => {
  let fixture: ComponentFixture<IdentityProvidersPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityProvidersPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            listProviders: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityProvidersPage);
    fixture.detectChanges();
  });

  it('renders providers section', () => {
    expect(fixture.nativeElement.textContent).toContain('Identity Providers');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
