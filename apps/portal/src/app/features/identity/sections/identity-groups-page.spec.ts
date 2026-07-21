import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentityGroupsPage } from './identity-groups-page';
import { IdentityService } from '../identity.service';

describe('GroupsComponentTest', () => {
  let fixture: ComponentFixture<IdentityGroupsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityGroupsPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            listGroups: () => of([]),
            getSummary: () => of(null),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityGroupsPage);
    fixture.detectChanges();
  });

  it('renders groups section', () => {
    expect(fixture.nativeElement.textContent).toContain('Groups');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
