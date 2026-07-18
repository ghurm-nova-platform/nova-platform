import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { UserSessionService } from '../../auth/services/user-session.service';
import { AuthUser } from '../../auth/services/auth.models';
import { CatalogModelFormPage } from './catalog-model-form-page';

describe('CatalogModelFormPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogModelFormPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
  });

  afterEach(() => {
    TestBed.inject(UserSessionService).clear();
  });

  it('requires model key and display name for create', () => {
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

    const fixture = TestBed.createComponent(CatalogModelFormPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.form.invalid).toBeTrue();
    component.form.patchValue({
      providerId: 'cccccccc-cccc-cccc-cccc-cccccccccc01',
      modelKey: 'GPT_4O',
      providerModelId: 'gpt-4o',
      displayName: 'GPT-4o',
    });
    expect(component.form.valid).toBeTrue();
  });
});
