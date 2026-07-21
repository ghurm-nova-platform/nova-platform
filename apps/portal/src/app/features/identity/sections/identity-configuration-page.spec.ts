import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { IdentityConfigurationPage } from './identity-configuration-page';
import { IdentityService } from '../identity.service';
import { IdentityConfigResponse } from '../identity.models';

describe('ConfigurationComponentTest', () => {
  let fixture: ComponentFixture<IdentityConfigurationPage>;

  const config: IdentityConfigResponse = {
    enabled: true,
    jwtAccessTtlSeconds: 900,
    jwtRefreshTtlSeconds: 604800,
    passwordMinLength: 12,
    passwordRequireUppercase: true,
    passwordRequireLowercase: true,
    passwordRequireDigit: true,
    passwordRequireSpecial: true,
    passwordMaxAgeDays: 90,
    sessionMaxConcurrent: 5,
    mfaRequired: true,
    scimEnabled: true,
    samlEnabled: true,
    oidcEnabled: false,
    ldapEnabled: false,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IdentityConfigurationPage],
      providers: [
        provideNoopAnimations(),
        {
          provide: IdentityService,
          useValue: {
            getConfig: () => of(config),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IdentityConfigurationPage);
    fixture.detectChanges();
  });

  it('renders configuration section', () => {
    expect(fixture.nativeElement.textContent).toContain('Configuration');
    expect(fixture.componentInstance.selectedMfaMethod).toBe('TOTP');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
