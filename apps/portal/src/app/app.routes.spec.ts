import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { routes } from './app.routes';
import { App } from './app';
import { UserSessionService } from './auth/services/user-session.service';
import { AuthUser } from './auth/services/auth.models';

describe('App routing', () => {
  const demoUser: AuthUser = {
    userId: '44444444-4444-4444-4444-444444444401',
    organizationId: '11111111-1111-1111-1111-111111111111',
    email: 'admin@nova.local',
    displayName: 'Nova Admin',
    roles: ['ORG_ADMIN'],
    permissions: ['AGENT_READ', 'AGENT_CREATE', 'AGENT_UPDATE', 'AGENT_ACTIVATE', 'AGENT_ARCHIVE'],
  };

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideAnimationsAsync()],
    }).compileComponents();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('redirects anonymous users from root to login', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    fixture.detectChanges();
    await router.navigateByUrl('/');
    fixture.detectChanges();
    expect(router.url).toBe('/login');
  });

  it('navigates authenticated users to administration routes', async () => {
    const session = TestBed.inject(UserSessionService);
    session.setSession(
      { accessToken: 'test-access', refreshToken: 'test-refresh' },
      demoUser,
    );

    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    fixture.detectChanges();

    for (const path of [
      '/dashboard',
      '/organizations',
      '/projects',
      '/agents',
      '/feedback',
      '/settings',
    ]) {
      await router.navigateByUrl(path);
      fixture.detectChanges();
      expect(router.url).toBe(path);
    }
  });
});
