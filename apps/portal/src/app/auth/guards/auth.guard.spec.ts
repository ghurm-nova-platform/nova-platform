import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router, provideRouter } from '@angular/router';

import { authGuard } from '../guards/auth.guard';
import { UserSessionService } from '../services/user-session.service';

describe('authGuard', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('redirects anonymous users to login', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), UserSessionService],
    });

    const router = TestBed.inject(Router);
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(result).toEqual(router.createUrlTree(['/login']));
  });
});
