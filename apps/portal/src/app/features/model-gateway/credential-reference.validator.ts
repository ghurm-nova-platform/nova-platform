import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const ENV_REFERENCE = /^env:NOVA_PROVIDER_[A-Z0-9_]+$/;
const VAULT_REFERENCE =
  /^vault:provider-secret:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
const BEARER = /bearer\s+/i;
const PRIVATE_KEY = /-----BEGIN (?:RSA )?PRIVATE KEY-----/i;
const JSON_SECRET = /^\s*\{.*"(?:api[_-]?key|secret|token)".*\}\s*$/is;
const PLAINTEXT_KEY = /(?:sk-[a-z0-9]{20,}|api[_-]?key\s*[:=])/i;

export function looksLikePlaintextSecret(value: string): boolean {
  return (
    BEARER.test(value) ||
    PRIVATE_KEY.test(value) ||
    JSON_SECRET.test(value) ||
    PLAINTEXT_KEY.test(value)
  );
}

export function isValidCredentialReference(value: string | null | undefined): boolean {
  if (value == null) {
    return true;
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return true;
  }
  if (looksLikePlaintextSecret(trimmed)) {
    return false;
  }
  return ENV_REFERENCE.test(trimmed) || VAULT_REFERENCE.test(trimmed);
}

export function credentialReferenceValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = typeof control.value === 'string' ? control.value : '';
    return isValidCredentialReference(value) ? null : { credentialReference: true };
  };
}
