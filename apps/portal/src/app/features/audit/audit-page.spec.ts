import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { UserSessionService } from '../../auth/services/user-session.service';
import { AuditPage } from './audit-page';
import { AuditPermissionHelper } from './audit-permission.helper';
import { AuditService } from './audit.service';
import { AuditEvent } from './audit.models';

describe('AuditPage', () => {
  const event: AuditEvent = {
    id: 'a1',
    organizationId: '11111111-1111-1111-1111-111111111111',
    entityType: 'ENVIRONMENT',
    entityId: 'e1',
    entityLabel: 'Staging',
    action: 'CREATE',
    result: 'SUCCESS',
    severity: 'MEDIUM',
    source: 'ENVIRONMENT_MANAGEMENT',
    createdAt: '2026-07-20T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({ roles: ['ORG_ADMIN'], permissions: ['AUDIT_READ'] }),
          },
        },
        {
          provide: AuditService,
          useValue: {
            list: () => of({ events: [event], total: 1, page: 0, size: 50 }),
            search: () => of({ events: [event], total: 1, page: 0, size: 50 }),
          },
        },
        AuditPermissionHelper,
      ],
    }).compileComponents();
  });

  it('loads recent events for authorized users', () => {
    const fixture = TestBed.createComponent(AuditPage);
    fixture.detectChanges();
    expect(fixture.componentInstance.events().length).toBe(1);
    expect(fixture.componentInstance.unauthorized()).toBeFalse();
  });
});
