import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentitySessionsPage } from './identity-sessions-page';
import { IdentityService } from '../identity.service';

describe('SessionsComponentTest', () => {
  let fixture: ComponentFixture<IdentitySessionsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentitySessionsPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            listSessions: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentitySessionsPage);
    fixture.detectChanges();
  });

  it('renders sessions section', () => {
    expect(fixture.nativeElement.textContent).toContain('Sessions');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
