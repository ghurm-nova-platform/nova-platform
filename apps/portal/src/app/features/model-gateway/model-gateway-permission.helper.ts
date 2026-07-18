import { Injectable, inject } from '@angular/core';

import { UserSessionService } from '../../auth/services/user-session.service';

const MODEL_GATEWAY_PERMISSIONS = {
  providerRead: 'MODEL_PROVIDER_READ',
  providerCreate: 'MODEL_PROVIDER_CREATE',
  providerUpdate: 'MODEL_PROVIDER_UPDATE',
  providerActivate: 'MODEL_PROVIDER_ACTIVATE',
  providerDisable: 'MODEL_PROVIDER_DISABLE',
  providerArchive: 'MODEL_PROVIDER_ARCHIVE',
  modelRead: 'MODEL_READ',
  modelCreate: 'MODEL_CREATE',
  modelUpdate: 'MODEL_UPDATE',
  modelActivate: 'MODEL_ACTIVATE',
  modelDisable: 'MODEL_DISABLE',
  modelArchive: 'MODEL_ARCHIVE',
  projectAssign: 'MODEL_PROJECT_ASSIGN',
  agentAssign: 'MODEL_AGENT_ASSIGN',
  routeRead: 'MODEL_ROUTE_READ',
  routeManage: 'MODEL_ROUTE_MANAGE',
  usageRead: 'MODEL_USAGE_READ',
} as const;

@Injectable({ providedIn: 'root' })
export class ModelGatewayPermissionHelper {
  private readonly session = inject(UserSessionService);

  canReadProviders(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.providerRead);
  }

  canCreateProvider(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.providerCreate);
  }

  canUpdateProvider(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.providerUpdate);
  }

  canActivateProvider(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.providerActivate);
  }

  canDisableProvider(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.providerDisable);
  }

  canArchiveProvider(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.providerArchive);
  }

  canReadModels(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.modelRead);
  }

  canCreateModel(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.modelCreate);
  }

  canUpdateModel(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.modelUpdate);
  }

  canActivateModel(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.modelActivate);
  }

  canDisableModel(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.modelDisable);
  }

  canArchiveModel(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.modelArchive);
  }

  canAssignProjectModels(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.projectAssign);
  }

  canAssignAgentModels(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.agentAssign);
  }

  canReadRoutingPolicies(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.routeRead);
  }

  canManageRoutingPolicies(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.routeManage);
  }

  canReadUsage(): boolean {
    return this.has(MODEL_GATEWAY_PERMISSIONS.usageRead);
  }

  private has(permission: string): boolean {
    const user = this.session.user();
    if (!user) {
      return false;
    }
    if (user.roles.includes('ORG_ADMIN')) {
      return true;
    }
    return (user.permissions ?? []).includes(permission);
  }
}
