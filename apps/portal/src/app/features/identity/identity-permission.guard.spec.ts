import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthUser } from '../../auth/services/auth.models';
import { UserSessionService } from '../../auth/services/user-session.service';
import { identityPermissionGuard } from './identity-permission.guard';

describe('PermissionGuardTest', () => {
  afterEach(() => {
    TestBed.inject(UserSessionService).clear();
  });

  it('redirects users without identity read to dashboard', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });

    const router = TestBed.inject(Router);
    const result = TestBed.runInInjectionContext(() =>
      identityPermissionGuard({} as never, {} as never),
    );

    expect(result).toEqual(router.createUrlTree(['/dashboard']));
  });

  it('allows ORG_ADMIN through identity permission guard', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });

    const session = TestBed.inject(UserSessionService);
    const admin: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444401',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'admin@nova.local',
      displayName: 'Nova Admin',
      roles: ['ORG_ADMIN'],
      permissions: [],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, admin);

    const result = TestBed.runInInjectionContext(() =>
      identityPermissionGuard({} as never, {} as never),
    );

    expect(result).toBeTrue();
  });

  it('allows IDENTITY_READ through identity permission guard', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });

    const session = TestBed.inject(UserSessionService);
    const member: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444402',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'member@nova.local',
      displayName: 'Nova Member',
      roles: ['PROJECT_MEMBER'],
      permissions: ['IDENTITY_READ'],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, member);

    const result = TestBed.runInInjectionContext(() =>
      identityPermissionGuard({} as never, {} as never),
    );

    expect(result).toBeTrue();
  });
});
