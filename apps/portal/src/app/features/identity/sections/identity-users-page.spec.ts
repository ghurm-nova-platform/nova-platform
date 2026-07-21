import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentityUsersPage } from './identity-users-page';
import { IdentityService } from '../identity.service';

describe('IdentityUsersComponentTest', () => {
  let fixture: ComponentFixture<IdentityUsersPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityUsersPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            listUsers: () => of([]),
            getSummary: () => of(null),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityUsersPage);
    fixture.detectChanges();
  });

  it('renders users section', () => {
    expect(fixture.nativeElement.textContent).toContain('Users');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
