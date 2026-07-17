import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { jwtInterceptor } from '../interceptors/jwt.interceptor';
import { RuntimeConfigService } from '../../core/config/runtime-config.service';
import { ApiClient } from '../../core/http/api-client';
import { AuthService } from './auth.service';
import { UserSessionService } from './user-session.service';

describe('AuthService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        RuntimeConfigService,
        ApiClient,
        AuthService,
        UserSessionService,
      ],
    });
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('stores tokens after login and never keeps the password', () => {
    const http = TestBed.inject(HttpTestingController);
    const auth = TestBed.inject(AuthService);
    const session = TestBed.inject(UserSessionService);

    let resolvedEmail = '';
    auth.login({ email: 'admin@nova.local', password: 'ChangeMe123!' }).subscribe((user) => {
      resolvedEmail = user.email;
    });

    const loginReq = http.expectOne('http://localhost:8080/api/auth/login');
    expect(loginReq.request.body).toEqual({
      email: 'admin@nova.local',
      password: 'ChangeMe123!',
    });
    loginReq.flush({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
    });

    const meReq = http.expectOne('http://localhost:8080/api/auth/me');
    expect(meReq.request.headers.get('Authorization')).toBe('Bearer access-token');
    meReq.flush({
      userId: '44444444-4444-4444-4444-444444444401',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'admin@nova.local',
      displayName: 'Nova Admin',
      roles: ['ORG_ADMIN'],
    });

    expect(resolvedEmail).toBe('admin@nova.local');
    expect(session.getAccessToken()).toBe('access-token');
    expect(sessionStorage.getItem('nova.accessToken')).toBe('access-token');
    expect(JSON.stringify(sessionStorage)).not.toContain('ChangeMe123!');
    http.verify();
  });
});
