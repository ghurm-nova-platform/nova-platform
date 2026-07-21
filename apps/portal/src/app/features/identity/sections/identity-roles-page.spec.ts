import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentityRolesPage } from './identity-roles-page';
import { IdentityService } from '../identity.service';

describe('RolesComponentTest', () => {
  let fixture: ComponentFixture<IdentityRolesPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityRolesPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            listRoles: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityRolesPage);
    fixture.detectChanges();
  });

  it('renders roles section', () => {
    expect(fixture.nativeElement.textContent).toContain('Roles');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
