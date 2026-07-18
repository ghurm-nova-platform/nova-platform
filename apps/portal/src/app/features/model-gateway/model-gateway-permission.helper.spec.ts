import { TestBed } from '@angular/core/testing';

import { UserSessionService } from '../../auth/services/user-session.service';
import { AuthUser } from '../../auth/services/auth.models';
import { ModelGatewayPermissionHelper } from './model-gateway-permission.helper';

describe('ModelGatewayPermissionHelper', () => {
  let helper: ModelGatewayPermissionHelper;
  let session: UserSessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    helper = TestBed.inject(ModelGatewayPermissionHelper);
    session = TestBed.inject(UserSessionService);
  });

  afterEach(() => {
    session.clear();
  });

  it('denies access when no user is signed in', () => {
    expect(helper.canReadProviders()).toBeFalse();
    expect(helper.canAssignProjectModels()).toBeFalse();
    expect(helper.canReadUsage()).toBeFalse();
  });

  it('grants all model gateway permissions to ORG_ADMIN', () => {
    const admin: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444401',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'admin@nova.local',
      displayName: 'Nova Admin',
      roles: ['ORG_ADMIN'],
      permissions: [],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, admin);

    expect(helper.canReadProviders()).toBeTrue();
    expect(helper.canCreateProvider()).toBeTrue();
    expect(helper.canReadModels()).toBeTrue();
    expect(helper.canAssignProjectModels()).toBeTrue();
    expect(helper.canAssignAgentModels()).toBeTrue();
    expect(helper.canManageRoutingPolicies()).toBeTrue();
    expect(helper.canReadUsage()).toBeTrue();
    expect(helper.canReadProviderSecrets()).toBeTrue();
    expect(helper.canCreateProviderSecret()).toBeTrue();
    expect(helper.canRotateProviderSecret()).toBeTrue();
    expect(helper.canRevokeProviderSecret()).toBeTrue();
    expect(helper.canTestProviderConnection()).toBeTrue();
  });

  it('checks explicit model gateway permissions for non-admin users', () => {
    const member: AuthUser = {
      userId: '44444444-4444-4444-4444-444444444402',
      organizationId: '11111111-1111-1111-1111-111111111111',
      email: 'member@nova.local',
      displayName: 'Nova Member',
      roles: ['PROJECT_MEMBER'],
      permissions: [
        'MODEL_READ',
        'MODEL_ROUTE_READ',
        'MODEL_USAGE_READ',
        'PROVIDER_SECRET_READ',
        'PROVIDER_CONNECTION_TEST',
      ],
    };
    session.setSession({ accessToken: 'token', refreshToken: 'refresh' }, member);

    expect(helper.canReadModels()).toBeTrue();
    expect(helper.canReadRoutingPolicies()).toBeTrue();
    expect(helper.canReadUsage()).toBeTrue();
    expect(helper.canReadProviderSecrets()).toBeTrue();
    expect(helper.canTestProviderConnection()).toBeTrue();
    expect(helper.canCreateProvider()).toBeFalse();
    expect(helper.canAssignAgentModels()).toBeFalse();
    expect(helper.canCreateProviderSecret()).toBeFalse();
    expect(helper.canRotateProviderSecret()).toBeFalse();
    expect(helper.canRevokeProviderSecret()).toBeFalse();
  });
});
