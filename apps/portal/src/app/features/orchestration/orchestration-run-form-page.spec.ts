import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { UserSessionService } from '../../auth/services/user-session.service';
import { AuthUser } from '../../auth/services/auth.models';
import { OrchestrationRunFormPage } from './orchestration-run-form-page';

describe('OrchestrationRunFormPage', () => {
  const projectId = '55555555-5555-5555-5555-555555555501';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrchestrationRunFormPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
  });

  afterEach(() => {
    TestBed.inject(UserSessionService).clear();
  });

  it('requires project, name, and objective for create', () => {
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

    const fixture = TestBed.createComponent(OrchestrationRunFormPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({
      projectId: '',
      name: '',
      objective: '',
    });
    expect(component.form.invalid).toBeTrue();

    component.form.patchValue({
      projectId,
      name: 'Demo orchestration',
      objective: 'Coordinate two agents',
    });
    expect(component.form.valid).toBeTrue();
  });
});
