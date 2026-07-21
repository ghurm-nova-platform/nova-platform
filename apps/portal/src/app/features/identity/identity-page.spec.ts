import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';
import { IdentityPage } from './identity-page';

describe('IdentityComponentTest', () => {
  let fixture: ComponentFixture<IdentityPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityPage],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: signal({
              roles: ['USER'],
              permissions: ['IDENTITY_READ', 'IDENTITY_ADMIN'],
            }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityPage);
    fixture.detectChanges();
  });

  it('renders Enterprise Identity heading', () => {
    expect(fixture.nativeElement.textContent).toContain('Enterprise Identity');
  });

  it('renders identity section navigation', () => {
    expect(fixture.nativeElement.textContent).toContain('Dashboard');
    expect(fixture.nativeElement.textContent).toContain('Users');
    expect(fixture.nativeElement.textContent).toContain('Configuration');
  });
});
