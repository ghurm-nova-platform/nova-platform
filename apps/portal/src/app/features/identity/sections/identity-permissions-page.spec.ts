import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentityPermissionsPage } from './identity-permissions-page';
import { IdentityService } from '../identity.service';

describe('PermissionsComponentTest', () => {
  let fixture: ComponentFixture<IdentityPermissionsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityPermissionsPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            listPermissions: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityPermissionsPage);
    fixture.detectChanges();
  });

  it('renders permissions section', () => {
    expect(fixture.nativeElement.textContent).toContain('Permissions');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
