import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentitySecurityEventsPage } from './identity-security-events-page';
import { IdentityService } from '../identity.service';

describe('SecurityEventsComponentTest', () => {
  let fixture: ComponentFixture<IdentitySecurityEventsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentitySecurityEventsPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            listSecurityEvents: () => of([]),
            loginHistory: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentitySecurityEventsPage);
    fixture.detectChanges();
  });

  it('renders security events section', () => {
    expect(fixture.nativeElement.textContent).toContain('Security Events');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
