import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { UserSessionService } from '../../auth/services/user-session.service';
import { KnowledgeEnginePermissionHelper } from './knowledge-engine-permission.helper';

describe('KnowledgeEnginePermissionHelper', () => {
  it('allows ORG_ADMIN without explicit permissions', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        KnowledgeEnginePermissionHelper,
        {
          provide: UserSessionService,
          useValue: { user: () => ({ roles: ['ORG_ADMIN'], permissions: [] }) },
        },
      ],
    });
    const helper = TestBed.inject(KnowledgeEnginePermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canWrite()).toBeTrue();
    expect(helper.canAdmin()).toBeTrue();
  });

  it('requires explicit permissions for non-admin users', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        KnowledgeEnginePermissionHelper,
        {
          provide: UserSessionService,
          useValue: {
            user: () => ({
              roles: ['MEMBER'],
              permissions: ['KNOWLEDGE_READ'],
            }),
          },
        },
      ],
    });
    const helper = TestBed.inject(KnowledgeEnginePermissionHelper);
    expect(helper.canRead()).toBeTrue();
    expect(helper.canWrite()).toBeFalse();
    expect(helper.canAdmin()).toBeFalse();
  });
});
