import { IdentityConfigResponse, IdentitySessionView } from './identity.models';

export function activeSessionCount(sessions: IdentitySessionView[]): number {
  return sessions.filter((session) => session.status === 'ACTIVE').length;
}

export function passwordPolicySummary(config: IdentityConfigResponse): string {
  const rules: string[] = [`min ${config.passwordMinLength} chars`];
  if (config.passwordRequireUppercase) {
    rules.push('uppercase');
  }
  if (config.passwordRequireLowercase) {
    rules.push('lowercase');
  }
  if (config.passwordRequireDigit) {
    rules.push('digit');
  }
  if (config.passwordRequireSpecial) {
    rules.push('special');
  }
  if (config.passwordMaxAgeDays > 0) {
    rules.push(`max age ${config.passwordMaxAgeDays} days`);
  }
  return rules.join(', ');
}

export function outcomeClass(outcome: string): string {
  return `identity__outcome identity__outcome--${outcome.toLowerCase()}`;
}

export function providerTypeIcon(type: string): string {
  switch (type) {
    case 'SAML':
      return 'security';
    case 'OIDC':
      return 'key';
    case 'LDAP':
      return 'folder_shared';
    default:
      return 'person';
  }
}

export function extractErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'error' in err) {
    const errorPayload = (err as { error?: { message?: string } }).error;
    if (errorPayload?.message) {
      return errorPayload.message;
    }
  }
  return fallback;
}
