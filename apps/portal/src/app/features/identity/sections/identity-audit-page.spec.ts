import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuditService } from '../../audit/audit.service';
import { IdentityAuditPage } from './identity-audit-page';
import { IdentityService } from '../identity.service';

describe('AuditComponentTest', () => {
  let fixture: ComponentFixture<IdentityAuditPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityAuditPage],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        {
          provide: AuditService,
          useValue: {
            search: () => of({ events: [], total: 0, page: 0, size: 50 }),
            list: () => of({ events: [], total: 0, page: 0, size: 50 }),
          },
        },
        {
          provide: IdentityService,
          useValue: {
            loginHistory: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityAuditPage);
    fixture.detectChanges();
  });

  it('renders audit section', () => {
    expect(fixture.nativeElement.textContent).toContain('Audit');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
